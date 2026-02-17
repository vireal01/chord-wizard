package com.vireal.chordwizard.bluetoothmidi

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PlatformBluetoothMidiService actual constructor() : BluetoothMidiService {
  private companion object {
    private const val SCAN_TIMEOUT_MILLIS = 12_000L
  }

  private val appContext = AndroidBluetoothMidiRuntime.requireContext()
  private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
  private val midiManager = appContext.getSystemService(MidiManager::class.java)
  private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
  private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner
  private val mainHandler = Handler(Looper.getMainLooper())

  private val stateMutex = Mutex()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val discoveredById = linkedMapOf<String, BluetoothMidiDevice>()
  private var activeScan = false
  private var classicReceiverRegistered = false
  private var scanTimeoutJob: Job? = null
  private var currentDevice: MidiDevice? = null
  private var currentOutputPort: MidiOutputPort? = null
  private var currentConnectedRef: BluetoothMidiDeviceRef? = null

  private val _availability = MutableStateFlow(initialAvailability())
  private val _scanState = MutableStateFlow<BluetoothMidiScanState>(BluetoothMidiScanState.Idle)
  private val _discoveredDevices = MutableStateFlow<List<BluetoothMidiDevice>>(emptyList())
  private val _connectionState =
    MutableStateFlow<BluetoothMidiConnectionState>(BluetoothMidiConnectionState.Disconnected)

  private val _incomingPackets =
    MutableSharedFlow<BluetoothMidiPacket>(
      extraBufferCapacity = 64,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  private val _incomingMessages =
    MutableSharedFlow<MidiMessageEvent>(
      extraBufferCapacity = 128,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  private val _noteEvents =
    MutableSharedFlow<NoteEvent>(
      extraBufferCapacity = 128,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  private val _errors =
    MutableSharedFlow<BluetoothMidiError>(
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  override val availability: StateFlow<BluetoothMidiAvailability> = _availability
  override val scanState: StateFlow<BluetoothMidiScanState> = _scanState
  override val discoveredDevices: StateFlow<List<BluetoothMidiDevice>> = _discoveredDevices
  override val connectionState: StateFlow<BluetoothMidiConnectionState> = _connectionState

  override val incomingPackets: Flow<BluetoothMidiPacket> = _incomingPackets
  override val incomingMessages: Flow<MidiMessageEvent> = _incomingMessages
  override val noteEvents: Flow<NoteEvent> = _noteEvents
  override val errors: Flow<BluetoothMidiError> = _errors

  private val scanCallback =
    object : ScanCallback() {
      override fun onScanResult(
        callbackType: Int,
        result: ScanResult,
      ) {
        handleScanResult(result)
      }

      override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach(::handleScanResult)
      }

      override fun onScanFailed(errorCode: Int) {
        val err = BluetoothMidiError.ScanFailed(message = "BLE scan failed with code: $errorCode")
        _scanState.value = BluetoothMidiScanState.Failed(error = err)
        _errors.tryEmit(err)
      }
    }

  private val classicDiscoveryReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?,
      ) {
        when (intent?.action) {
          BluetoothDevice.ACTION_FOUND -> {
            val device =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
              } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
              }
            if (device != null) {
              upsertDevice(device.toBluetoothMidiDevice(transport = BluetoothMidiTransport.BLUETOOTH))
            }
          }
          BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
            // No-op. BLE scan may still be active.
          }
        }
      }
    }

  private val midiReceiver =
    object : MidiReceiver() {
      override fun onSend(
        msg: ByteArray,
        offset: Int,
        count: Int,
        timestamp: Long,
      ) {
        val ref = currentConnectedRef ?: return
        if (offset < 0 || count <= 0 || offset + count > msg.size) {
          return
        }

        val payload = msg.copyOfRange(offset, offset + count)
        val receivedAt = if (timestamp > 0L) timestamp / 1_000_000L else System.currentTimeMillis()
        val packet =
          BluetoothMidiPacket(
            device = ref,
            bytes = payload,
            receivedAtEpochMillis = receivedAt,
          )

        _incomingPackets.tryEmit(packet)
        val parsed = MidiMessageParser.parse(packet)
        parsed.forEach { event ->
          _incomingMessages.tryEmit(event)
          MidiMessageParser.toNoteEvents(event).forEach(_noteEvents::tryEmit)
        }
      }
    }

  override suspend fun refreshAvailability() {
    _availability.value = initialAvailability()
  }

  override suspend fun startScan() {
    stateMutex.withLock {
      val availabilityNow = initialAvailability()
      _availability.value = availabilityNow
      if (availabilityNow.status != BluetoothMidiAvailability.Status.AVAILABLE) {
        val err =
          BluetoothMidiError.PermissionDenied(
            missing = availabilityNow.missingPermissions,
            message = availabilityNow.details ?: "Bluetooth MIDI is not available.",
          )
        _scanState.value = BluetoothMidiScanState.Failed(error = err)
        _errors.tryEmit(err)
        return
      }

      if (activeScan) {
        return
      }

      discoveredById.clear()
      _discoveredDevices.value = emptyList()
      refreshBondedDevices()

      var startedAny = false
      startedAny = startBleScanInternal() || startedAny
      startedAny = startClassicDiscoveryInternal() || startedAny

      if (!startedAny && discoveredById.isEmpty()) {
        val err = BluetoothMidiError.ScanFailed(message = "Unable to start Bluetooth device discovery.")
        _scanState.value = BluetoothMidiScanState.Failed(error = err)
        _errors.tryEmit(err)
        return
      }

      activeScan = true
      _scanState.value = BluetoothMidiScanState.Scanning(startedAtEpochMillis = System.currentTimeMillis())
      scheduleScanTimeout()
    }
  }

  override suspend fun stopScan() {
    stateMutex.withLock {
      stopScanInternal(BluetoothMidiScanState.Stopped.StopReason.USER)
    }
  }

  override suspend fun connect(
    deviceId: String,
    config: BluetoothMidiConnectionConfig,
  ) {
    stateMutex.withLock {
      val availabilityNow = initialAvailability()
      _availability.value = availabilityNow
      if (availabilityNow.status != BluetoothMidiAvailability.Status.AVAILABLE) {
        val err =
          BluetoothMidiError.PermissionDenied(
            missing = availabilityNow.missingPermissions,
            message = availabilityNow.details ?: "Bluetooth MIDI is not available.",
          )
        _connectionState.value = BluetoothMidiConnectionState.Failed(target = null, error = err)
        _errors.tryEmit(err)
        return
      }

      val btAdapter = adapter
      if (btAdapter == null) {
        val err = BluetoothMidiError.UnsupportedPlatform(message = "Bluetooth adapter is unavailable.")
        _connectionState.value = BluetoothMidiConnectionState.Failed(target = null, error = err)
        _errors.tryEmit(err)
        return
      }

      val remote =
        try {
          btAdapter.getRemoteDevice(deviceId)
        } catch (_: IllegalArgumentException) {
          null
        }

      if (remote == null) {
        val err = BluetoothMidiError.ConnectionFailed(deviceId, "Unknown Bluetooth device id: $deviceId")
        _connectionState.value = BluetoothMidiConnectionState.Failed(target = null, error = err)
        _errors.tryEmit(err)
        return
      }

      val targetRef = BluetoothMidiDeviceRef(id = remote.address, name = remote.name)
      _connectionState.value = BluetoothMidiConnectionState.Connecting(target = targetRef)

      closeCurrentConnection()

      val opened =
        withTimeoutOrNull(config.connectTimeoutMillis) {
          openMidiDevice(remote)
        }

      if (opened == null) {
        val err = BluetoothMidiError.ConnectionFailed(deviceId, "Connection timed out.")
        _connectionState.value = BluetoothMidiConnectionState.Failed(target = targetRef, error = err)
        _errors.tryEmit(err)
        return
      }

      val outputPort = opened.openOutputPort(0)
      if (outputPort == null) {
        opened.close()
        val err =
          BluetoothMidiError.ConnectionFailed(
            deviceId,
            "MIDI output port is unavailable on connected device.",
          )
        _connectionState.value = BluetoothMidiConnectionState.Failed(target = targetRef, error = err)
        _errors.tryEmit(err)
        return
      }

      outputPort.connect(midiReceiver)
      currentDevice = opened
      currentOutputPort = outputPort

      val model =
        discoveredById[deviceId]
          ?: remote.toBluetoothMidiDevice(transport = BluetoothMidiTransport.UNKNOWN)
      currentConnectedRef = BluetoothMidiDeviceRef(id = model.id, name = model.name)
      _connectionState.value = BluetoothMidiConnectionState.Connected(device = model)
    }
  }

  override suspend fun disconnect() {
    stateMutex.withLock {
      val connected = _connectionState.value
      val ref =
        when (connected) {
          is BluetoothMidiConnectionState.Connected -> {
            BluetoothMidiDeviceRef(id = connected.device.id, name = connected.device.name)
          }

          is BluetoothMidiConnectionState.Connecting -> {
            connected.target
          }

          is BluetoothMidiConnectionState.Disconnecting -> {
            connected.device
          }

          else -> {
            null
          }
        }

      if (ref != null) {
        _connectionState.value = BluetoothMidiConnectionState.Disconnecting(device = ref)
      }
      closeCurrentConnection()
      _connectionState.value = BluetoothMidiConnectionState.Disconnected
    }
  }

  private suspend fun openMidiDevice(device: BluetoothDevice): MidiDevice? =
    suspendCoroutine { continuation ->
      try {
        midiManager.openBluetoothDevice(
          device,
          { midiDevice -> continuation.resume(midiDevice) },
          mainHandler,
        )
      } catch (_: SecurityException) {
        val err =
          BluetoothMidiError.PermissionDenied(
            missing = missingPermissions(),
            message = "Bluetooth connect permission is missing.",
          )
        _errors.tryEmit(err)
        continuation.resume(null)
      } catch (t: Throwable) {
        _errors.tryEmit(
          BluetoothMidiError.ConnectionFailed(
            device.address,
            t.message ?: "Unable to open BLE MIDI device.",
          ),
        )
        continuation.resume(null)
      }
    }

  private fun closeCurrentConnection() {
    try {
      currentOutputPort?.disconnect(midiReceiver)
    } catch (_: Throwable) {
    }
    try {
      currentOutputPort?.close()
    } catch (_: Throwable) {
    }
    try {
      currentDevice?.close()
    } catch (_: Throwable) {
    }
    currentOutputPort = null
    currentDevice = null
    currentConnectedRef = null
  }

  private fun stopScanInternal(reason: BluetoothMidiScanState.Stopped.StopReason) {
    scanTimeoutJob?.cancel()
    scanTimeoutJob = null

    if (!activeScan) {
      return
    }

    try {
      scanner?.stopScan(scanCallback)
    } catch (_: Throwable) {
    }
    try {
      if (adapter?.isDiscovering == true) {
        adapter.cancelDiscovery()
      }
    } catch (_: Throwable) {
    }
    activeScan = false
    _scanState.value =
      BluetoothMidiScanState.Stopped(
        stoppedAtEpochMillis = System.currentTimeMillis(),
        reason = reason,
      )
  }

  private fun handleScanResult(result: ScanResult) {
    val device = result.device ?: return
    val model =
      device.toBluetoothMidiDevice(
        transport = BluetoothMidiTransport.BLUETOOTH,
        rssi = result.rssi,
        isConnectable = result.isConnectableCompat(),
      )
    upsertDevice(model)
  }

  private fun initialAvailability(): BluetoothMidiAvailability {
    if (midiManager == null || adapter == null) {
      return BluetoothMidiAvailability(
        status = BluetoothMidiAvailability.Status.UNSUPPORTED,
        details = "Bluetooth MIDI API is unavailable on this device.",
      )
    }

    if (adapter.isEnabled.not()) {
      return BluetoothMidiAvailability(
        status = BluetoothMidiAvailability.Status.UNAVAILABLE,
        details = "Bluetooth is disabled.",
      )
    }

    val missing = missingPermissions()
    if (missing.isNotEmpty()) {
      return BluetoothMidiAvailability(
        status = BluetoothMidiAvailability.Status.PERMISSION_REQUIRED,
        missingPermissions = missing,
        details = "Required Bluetooth permissions are missing.",
      )
    }

    return BluetoothMidiAvailability(status = BluetoothMidiAvailability.Status.AVAILABLE)
  }

  private fun missingPermissions(): Set<BluetoothMidiPermission> {
    val missing = linkedSetOf<BluetoothMidiPermission>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
        missing += BluetoothMidiPermission.BLUETOOTH_SCAN
      }
      if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
        missing += BluetoothMidiPermission.BLUETOOTH_CONNECT
      }
    } else {
      if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
        missing += BluetoothMidiPermission.LOCATION
      }
    }

    return missing
  }

  private fun hasPermission(permission: String): Boolean =
    appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

  private fun startBleScanInternal(): Boolean {
    val bleScanner = scanner ?: return false
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    return try {
      bleScanner.startScan(null, settings, scanCallback)
      true
    } catch (_: SecurityException) {
      val err =
        BluetoothMidiError.PermissionDenied(
          missing = missingPermissions(),
          message = "Bluetooth scan permission is missing.",
        )
      _errors.tryEmit(err)
      false
    } catch (t: Throwable) {
      _errors.tryEmit(BluetoothMidiError.ScanFailed(message = t.message ?: "BLE scan start failed."))
      false
    }
  }

  private fun startClassicDiscoveryInternal(): Boolean {
    val btAdapter = adapter ?: return false
    return try {
      ensureClassicReceiverRegistered()
      if (btAdapter.isDiscovering) {
        btAdapter.cancelDiscovery()
      }
      btAdapter.startDiscovery()
    } catch (_: SecurityException) {
      false
    } catch (_: Throwable) {
      false
    }
  }

  private fun refreshBondedDevices() {
    val btAdapter = adapter ?: return
    try {
      btAdapter.bondedDevices
        ?.map { bonded ->
          bonded.toBluetoothMidiDevice(transport = BluetoothMidiTransport.BLUETOOTH)
        }?.forEach(::upsertDevice)
    } catch (_: SecurityException) {
      // Ignore; availability/permissions are surfaced elsewhere.
    }
  }

  private fun upsertDevice(device: BluetoothMidiDevice) {
    discoveredById[device.id] = device
    _discoveredDevices.value = discoveredById.values.toList()
  }

  private fun ensureClassicReceiverRegistered() {
    if (classicReceiverRegistered) {
      return
    }
    val filter =
      IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
      }
    appContext.registerReceiver(classicDiscoveryReceiver, filter)
    classicReceiverRegistered = true
  }

  private fun scheduleScanTimeout() {
    scanTimeoutJob?.cancel()
    scanTimeoutJob =
      scope.launch {
        delay(SCAN_TIMEOUT_MILLIS)
        stateMutex.withLock {
          if (!activeScan) {
            return@withLock
          }
          val noDevices = discoveredById.isEmpty()
          stopScanInternal(BluetoothMidiScanState.Stopped.StopReason.TIMEOUT)
          if (noDevices) {
            _errors.tryEmit(
              BluetoothMidiError.ScanFailed(
                message = "No Bluetooth devices found. Verify pairing/discovery mode on your MIDI keyboard.",
              ),
            )
          }
        }
      }
  }
}

private fun BluetoothDevice.toBluetoothMidiDevice(
  transport: BluetoothMidiTransport,
  rssi: Int? = null,
  isConnectable: Boolean = true,
): BluetoothMidiDevice =
  BluetoothMidiDevice(
    id = address,
    name = name,
    rssi = rssi,
    isConnectable = isConnectable,
    transport = transport,
    lastSeenEpochMillis = System.currentTimeMillis(),
  )

private fun ScanResult.isConnectableCompat(): Boolean =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    isConnectable
  } else {
    true
  }

object AndroidBluetoothMidiRuntime {
  @Volatile
  private var context: Context? = null

  fun initialize(context: Context) {
    this.context = context.applicationContext
  }

  fun requireContext(): Context =
    context
      ?: error(
        "AndroidBluetoothMidiRuntime is not initialized. " +
          "Call AndroidBluetoothMidiRuntime.initialize(context) at app startup.",
      )
}
