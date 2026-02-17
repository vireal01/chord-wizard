package com.vireal.chordwizard.midi.usb

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceStatus
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionConfig
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiDeviceRef
import com.vireal.chordwizard.midi.core.MidiError
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.core.MidiMessageEvent
import com.vireal.chordwizard.midi.core.MidiMessageParser
import com.vireal.chordwizard.midi.core.MidiPacket
import com.vireal.chordwizard.midi.core.MidiScanState
import com.vireal.chordwizard.midi.core.MidiTransport
import com.vireal.chordwizard.midi.core.NoteEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PlatformUsbMidiInputService actual constructor() : MidiInputService {
  private val appContext = AndroidUsbMidiRuntime.requireContext()
  private val midiManager = appContext.getSystemService(MidiManager::class.java)
  private val mainHandler = Handler(Looper.getMainLooper())

  private val stateMutex = Mutex()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val discoveredById = linkedMapOf<String, com.vireal.chordwizard.midi.core.MidiDevice>()
  private var callbackRegistered = false
  private var scanSubscribers = 0
  private var currentDevice: MidiDevice? = null
  private var currentOutputPort: MidiOutputPort? = null
  private var currentConnectedRef: MidiDeviceRef? = null

  private val _availability = MutableStateFlow(initialAvailability())
  private val _scanState = MutableStateFlow<MidiScanState>(MidiScanState.Idle)
  private val _discoveredDevices = MutableStateFlow<List<com.vireal.chordwizard.midi.core.MidiDevice>>(emptyList())
  private val _connectionState = MutableStateFlow<MidiConnectionState>(MidiConnectionState.Disconnected)

  private val _incomingPackets =
    MutableSharedFlow<MidiPacket>(
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
    MutableSharedFlow<MidiError>(
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  override val availability: StateFlow<MidiAvailability> = _availability
  override val scanState: StateFlow<MidiScanState> = _scanState
  override val discoveredDevices: StateFlow<List<com.vireal.chordwizard.midi.core.MidiDevice>> = _discoveredDevices
  override val connectionState: StateFlow<MidiConnectionState> = _connectionState

  override val incomingPackets: Flow<MidiPacket> = _incomingPackets
  override val incomingMessages: Flow<MidiMessageEvent> = _incomingMessages
  override val noteEvents: Flow<NoteEvent> = _noteEvents
  override val errors: Flow<MidiError> = _errors

  private val deviceCallback =
    object : MidiManager.DeviceCallback() {
      override fun onDeviceAdded(device: MidiDeviceInfo) {
        if (device.type == MidiDeviceInfo.TYPE_USB) {
          upsertDevice(device.toMidiDevice())
        }
      }

      override fun onDeviceRemoved(device: MidiDeviceInfo) {
        discoveredById.remove(device.id.toString())
        _discoveredDevices.value = discoveredById.values.toList()
      }

      override fun onDeviceStatusChanged(
        status: MidiDeviceStatus,
      ) {
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
        val packet = MidiPacket(device = ref, bytes = payload, receivedAtEpochMillis = receivedAt)

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
      _availability.value = initialAvailability()
      if (_availability.value.status != MidiAvailability.Status.AVAILABLE) {
        return
      }

      scanSubscribers += 1
      if (scanSubscribers > 1) {
        return
      }

      discoveredById.clear()
      refreshUsbDevices()
      val callbackReady = registerDeviceCallbackIfNeeded()
      if (!callbackReady) {
        scanSubscribers = 0
        return
      }
      _scanState.value = MidiScanState.Scanning(startedAtEpochMillis = System.currentTimeMillis())
    }
  }

  override suspend fun stopScan() {
    stateMutex.withLock {
      if (scanSubscribers > 0) {
        scanSubscribers -= 1
      }

      if (scanSubscribers > 0) {
        return
      }

      unregisterDeviceCallbackIfNeeded()
      _scanState.value =
        MidiScanState.Stopped(
          stoppedAtEpochMillis = System.currentTimeMillis(),
          reason = MidiScanState.Stopped.StopReason.USER,
        )
    }
  }

  override suspend fun connect(
    deviceId: String,
    config: MidiConnectionConfig,
  ) {
    stateMutex.withLock {
      val target = discoveredById[deviceId]
      if (target == null) {
        val err = MidiError.ConnectionFailed(deviceId, "Unknown USB MIDI device id: $deviceId")
        _connectionState.value = MidiConnectionState.Failed(target = null, error = err)
        _errors.tryEmit(err)
        return
      }

      _connectionState.value = MidiConnectionState.Connecting(target = target.ref())
      closeCurrentConnection()

      val info = midiManager.devices.firstOrNull { it.id.toString() == deviceId }
      if (info == null) {
        val err = MidiError.ConnectionFailed(deviceId, "USB MIDI device is no longer available.")
        _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
        _errors.tryEmit(err)
        return
      }

      val opened = withTimeoutOrNull(config.connectTimeoutMillis) { openDevice(info) }
      if (opened == null) {
        val err = MidiError.ConnectionFailed(deviceId, "Connection timed out.")
        _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
        _errors.tryEmit(err)
        return
      }

      val outputPort = opened.openOutputPort(0)
      if (outputPort == null) {
        opened.close()
        val err = MidiError.ConnectionFailed(deviceId, "MIDI output port is unavailable.")
        _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
        _errors.tryEmit(err)
        return
      }

      outputPort.connect(midiReceiver)
      currentDevice = opened
      currentOutputPort = outputPort
      currentConnectedRef = target.ref()
      _connectionState.value = MidiConnectionState.Connected(target)
    }
  }

  override suspend fun disconnect() {
    stateMutex.withLock {
      val current = connectionState.value
      val ref =
        when (current) {
          is MidiConnectionState.Connected -> current.device.ref()
          is MidiConnectionState.Connecting -> current.target
          is MidiConnectionState.Disconnecting -> current.device
          else -> null
        }

      if (ref != null) {
        _connectionState.value = MidiConnectionState.Disconnecting(ref)
      }
      closeCurrentConnection()
      _connectionState.value = MidiConnectionState.Disconnected
    }
  }

  private suspend fun openDevice(info: MidiDeviceInfo): MidiDevice? =
    suspendCoroutine { continuation ->
      try {
        midiManager.openDevice(
          info,
          { opened -> continuation.resume(opened) },
          mainHandler,
        )
      } catch (t: Throwable) {
        _errors.tryEmit(MidiError.ConnectionFailed(info.id.toString(), t.message ?: "Failed to open MIDI device."))
        continuation.resume(null)
      }
    }

  private fun refreshUsbDevices() {
    midiManager.devices
      .filter { it.type == MidiDeviceInfo.TYPE_USB }
      .map { it.toMidiDevice() }
      .forEach(::upsertDevice)
  }

  private fun registerDeviceCallbackIfNeeded(): Boolean {
    if (callbackRegistered) {
      return true
    }
    try {
      midiManager.registerDeviceCallback(deviceCallback, mainHandler)
      callbackRegistered = true
      return true
    } catch (t: Throwable) {
      val error =
        MidiError.ScanFailed(
          message = "Failed to register USB MIDI device callback: ${t.message ?: "unknown error"}",
        )
      _scanState.value = MidiScanState.Failed(error)
      _errors.tryEmit(error)
      return false
    }
  }

  private fun unregisterDeviceCallbackIfNeeded() {
    if (!callbackRegistered) {
      return
    }
    try {
      midiManager.unregisterDeviceCallback(deviceCallback)
      callbackRegistered = false
    } catch (t: Throwable) {
      val error =
        MidiError.ScanFailed(
          message = "Failed to unregister USB MIDI device callback: ${t.message ?: "unknown error"}",
        )
      _errors.tryEmit(error)
    }
  }

  private fun upsertDevice(device: com.vireal.chordwizard.midi.core.MidiDevice) {
    discoveredById[device.id] = device
    _discoveredDevices.value = discoveredById.values.sortedBy { it.name ?: it.id }
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

  private fun initialAvailability(): MidiAvailability {
    if (midiManager == null) {
      return MidiAvailability(
        status = MidiAvailability.Status.UNSUPPORTED,
        details = "Android MIDI API is unavailable on this device.",
      )
    }
    return MidiAvailability(status = MidiAvailability.Status.AVAILABLE)
  }
}

private fun MidiDeviceInfo.toMidiDevice(): com.vireal.chordwizard.midi.core.MidiDevice {
  val props: Bundle = properties
  val name = props.getString(MidiDeviceInfo.PROPERTY_NAME)
  val manufacturer = props.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
  val product = props.getString(MidiDeviceInfo.PROPERTY_PRODUCT)

  return com.vireal.chordwizard.midi.core.MidiDevice(
    id = id.toString(),
    name = name ?: product ?: "USB MIDI Device #$id",
    transport = MidiTransport.USB,
    manufacturer = manufacturer,
    product = product,
    lastSeenEpochMillis = System.currentTimeMillis(),
    isConnectable = true,
  )
}

private fun com.vireal.chordwizard.midi.core.MidiDevice.ref(): MidiDeviceRef =
  MidiDeviceRef(id = id, name = name)

object AndroidUsbMidiRuntime {
  @Volatile
  private var context: Context? = null

  fun initialize(context: Context) {
    this.context = context.applicationContext
  }

  fun requireContext(): Context =
    context
      ?: error(
        "AndroidUsbMidiRuntime is not initialized. " +
          "Call AndroidUsbMidiRuntime.initialize(context) at app startup.",
      )
}
