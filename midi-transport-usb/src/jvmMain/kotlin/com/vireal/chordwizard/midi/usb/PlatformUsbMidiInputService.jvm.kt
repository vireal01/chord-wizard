package com.vireal.chordwizard.midi.usb

import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionConfig
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiDevice
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import javax.sound.midi.MidiMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.Transmitter
import javax.sound.midi.MidiDevice as JvmMidiDevice

actual class PlatformUsbMidiInputService actual constructor() : MidiInputService {
  private val stateMutex = Mutex()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val discoveredById = linkedMapOf<String, MidiDevice>()
  private val deviceInfoById = linkedMapOf<String, JvmMidiDevice.Info>()
  private val deviceSignatureById = linkedMapOf<String, String>()
  private val nextOrdinalBySignature = linkedMapOf<String, Int>()

  private var scanSubscribers = 0
  private var scanJob: Job? = null
  private var currentDeviceId: String? = null
  private var currentDevice: JvmMidiDevice? = null
  private var currentTransmitter: Transmitter? = null
  private var currentConnectedRef: MidiDeviceRef? = null
  private var currentConnectedSignature: String? = null

  private val _availability = MutableStateFlow(initialAvailability())
  private val _scanState = MutableStateFlow<MidiScanState>(MidiScanState.Idle)
  private val _discoveredDevices = MutableStateFlow<List<MidiDevice>>(emptyList())
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
  override val discoveredDevices: StateFlow<List<MidiDevice>> = _discoveredDevices
  override val connectionState: StateFlow<MidiConnectionState> = _connectionState
  override val incomingPackets: Flow<MidiPacket> = _incomingPackets
  override val incomingMessages: Flow<MidiMessageEvent> = _incomingMessages
  override val noteEvents: Flow<NoteEvent> = _noteEvents
  override val errors: Flow<MidiError> = _errors

  private val midiReceiver =
    object : Receiver {
      override fun send(
        message: MidiMessage,
        timeStamp: Long,
      ) {
        val ref = currentConnectedRef ?: return
        val payload = message.payloadOrNull() ?: return
        val receivedAt =
          if (timeStamp >= 0L) {
            timeStamp / 1_000L
          } else {
            System.currentTimeMillis()
          }

        val packet = MidiPacket(device = ref, bytes = payload, receivedAtEpochMillis = receivedAt)
        _incomingPackets.tryEmit(packet)
        val parsed = MidiMessageParser.parse(packet)
        parsed.forEach { event ->
          _incomingMessages.tryEmit(event)
          MidiMessageParser.toNoteEvents(event).forEach(_noteEvents::tryEmit)
        }
      }

      override fun close() {
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

      refreshInputDevices()
      scanJob =
        scope.launch {
          while (isActive) {
            delay(SCAN_REFRESH_INTERVAL_MILLIS)
            stateMutex.withLock {
              refreshInputDevices()
            }
          }
        }
      _scanState.value = MidiScanState.Scanning(startedAtEpochMillis = System.currentTimeMillis())
    }
  }

  override suspend fun stopScan() {
    var jobToCancel: Job? = null
    stateMutex.withLock {
      if (scanSubscribers > 0) {
        scanSubscribers -= 1
      }

      if (scanSubscribers > 0) {
        return
      }

      jobToCancel = scanJob
      scanJob = null
      _scanState.value =
        MidiScanState.Stopped(
          stoppedAtEpochMillis = System.currentTimeMillis(),
          reason = MidiScanState.Stopped.StopReason.USER,
        )
    }
    jobToCancel?.cancelAndJoin()
  }

  override suspend fun connect(
    deviceId: String,
    config: MidiConnectionConfig,
  ) {
    stateMutex.withLock {
      val target = discoveredById[deviceId]
      if (target == null) {
        val err = MidiError.ConnectionFailed(deviceId, "Unknown MIDI input device id: $deviceId")
        _connectionState.value = MidiConnectionState.Failed(target = null, error = err)
        _errors.tryEmit(err)
        return
      }

      _connectionState.value = MidiConnectionState.Connecting(target.ref())
      closeCurrentConnection()

      val info = deviceInfoById[deviceId]
      if (info == null) {
        val err = MidiError.ConnectionFailed(deviceId, "MIDI input device is no longer available.")
        _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
        _errors.tryEmit(err)
        return
      }

      val opened =
        when (val openResult = openDevice(info = info, timeoutMillis = config.connectTimeoutMillis)) {
          OpenDeviceResult.Timeout -> {
            val err = MidiError.ConnectionFailed(deviceId, "Connection timed out.")
            _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
            _errors.tryEmit(err)
            return
          }

          OpenDeviceResult.Failed -> {
            _connectionState.value =
              MidiConnectionState.Failed(
                target = target.ref(),
                error = MidiError.ConnectionFailed(deviceId, "Failed to open MIDI input device."),
              )
            return
          }

          is OpenDeviceResult.Opened -> openResult.device
        }

      val transmitter = runCatching { opened.transmitter }.getOrNull()
      if (transmitter == null) {
        runCatching { opened.close() }
        val err = MidiError.ConnectionFailed(deviceId, "MIDI transmitter is unavailable.")
        _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
        _errors.tryEmit(err)
        return
      }

      runCatching { transmitter.receiver = midiReceiver }
        .onFailure { throwable ->
          runCatching { transmitter.close() }
          runCatching { opened.close() }
          val err = MidiError.ConnectionFailed(deviceId, throwable.message ?: "Failed to attach MIDI receiver.")
          _connectionState.value = MidiConnectionState.Failed(target = target.ref(), error = err)
          _errors.tryEmit(err)
          return@withLock
        }

      currentDeviceId = deviceId
      currentDevice = opened
      currentTransmitter = transmitter
      currentConnectedRef = target.ref()
      currentConnectedSignature = info.signature()
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

  private fun refreshInputDevices() {
    val queried = queryInputDevices()
    val connectedSignature = currentConnectedSignature
    val connectedId = currentDeviceId
    val previousIdsBySignature =
      deviceSignatureById.entries
        .groupBy(keySelector = { it.value }, valueTransform = { it.key })
        .mapValues { (_, ids) -> ArrayDeque(ids) }

    discoveredById.clear()
    deviceInfoById.clear()
    deviceSignatureById.clear()
    queried.forEach { entry ->
      val id = previousIdsBySignature[entry.signature]?.removeFirstOrNull() ?: createStableId(entry.signature)
      discoveredById[id] =
        MidiDevice(
          id = id,
          name = entry.name,
          transport = MidiTransport.USB,
          manufacturer = entry.manufacturer,
          product = entry.product,
          isConnectable = true,
          lastSeenEpochMillis = entry.lastSeenEpochMillis,
        )
      deviceInfoById[id] = entry.info
      deviceSignatureById[id] = entry.signature
    }
    _discoveredDevices.value = discoveredById.values.sortedBy { it.name ?: it.id }

    if (connectedSignature != null && queried.none { it.signature == connectedSignature }) {
      closeCurrentConnection()
      _connectionState.value = MidiConnectionState.Disconnected
      val err =
        MidiError.ConnectionFailed(
          deviceId = connectedId,
          message = "Connected MIDI device was removed.",
        )
      _errors.tryEmit(err)
    }
  }

  private fun queryInputDevices(): List<DiscoveredJvmMidiDevice> {
    val infos =
      runCatching { MidiSystem.getMidiDeviceInfo().toList() }
        .onFailure { throwable ->
          val error =
            MidiError.ScanFailed(
              "Failed to enumerate JVM MIDI devices: ${throwable.message ?: "unknown error"}",
            )
          _scanState.value = MidiScanState.Failed(error)
          _errors.tryEmit(error)
        }
        .getOrElse { return emptyList() }

    val now = System.currentTimeMillis()
    return infos.mapNotNull { info ->
      val device =
        runCatching { MidiSystem.getMidiDevice(info) }
          .getOrNull()
          ?: return@mapNotNull null

      if (!device.supportsIncomingData()) {
        return@mapNotNull null
      }

      DiscoveredJvmMidiDevice(
        info = info,
        signature = info.signature(),
        name = info.name?.takeUnless { it.isBlank() } ?: info.description,
        manufacturer = info.vendor?.takeUnless { it.isBlank() },
        product = info.description?.takeUnless { it.isBlank() },
        lastSeenEpochMillis = now,
      )
    }
  }

  private suspend fun openDevice(
    info: JvmMidiDevice.Info,
    timeoutMillis: Long,
  ): OpenDeviceResult {
    if (timeoutMillis <= 0L) {
      return OpenDeviceResult.Timeout
    }

    return withTimeoutOrNull(timeoutMillis) {
      withContext(Dispatchers.IO) {
        runCatching {
          val device = MidiSystem.getMidiDevice(info)
          if (!device.isOpen) {
            device.open()
          }
          OpenDeviceResult.Opened(device)
        }.getOrElse { throwable ->
          _errors.tryEmit(
            MidiError.ConnectionFailed(
              deviceId = null,
              message = throwable.message ?: "Failed to open MIDI input device.",
            ),
          )
          OpenDeviceResult.Failed
        }
      }
    } ?: OpenDeviceResult.Timeout
  }

  private fun closeCurrentConnection() {
    runCatching { currentTransmitter?.receiver = null }
    runCatching { currentTransmitter?.close() }
    runCatching { currentDevice?.close() }
    currentTransmitter = null
    currentDevice = null
    currentDeviceId = null
    currentConnectedRef = null
    currentConnectedSignature = null
  }

  private fun initialAvailability(): MidiAvailability {
    return runCatching { MidiSystem.getMidiDeviceInfo() }
      .fold(
        onSuccess = {
          MidiAvailability(status = MidiAvailability.Status.AVAILABLE)
        },
        onFailure = { throwable ->
          MidiAvailability(
            status = MidiAvailability.Status.UNSUPPORTED,
            details = throwable.message ?: "JVM MIDI API is unavailable on this runtime.",
          )
        },
      )
  }

  private data class DiscoveredJvmMidiDevice(
    val info: JvmMidiDevice.Info,
    val signature: String,
    val name: String?,
    val manufacturer: String?,
    val product: String?,
    val lastSeenEpochMillis: Long,
  )

  private companion object {
    const val SCAN_REFRESH_INTERVAL_MILLIS = 1_000L
  }

  private sealed interface OpenDeviceResult {
    data object Timeout : OpenDeviceResult

    data object Failed : OpenDeviceResult

    data class Opened(
      val device: JvmMidiDevice,
    ) : OpenDeviceResult
  }

  private fun createStableId(signature: String): String {
    val next = (nextOrdinalBySignature[signature] ?: 0) + 1
    nextOrdinalBySignature[signature] = next
    return "jvm-midi-${signature.hashCode().toUInt().toString(16)}-$next"
  }
}

private fun JvmMidiDevice.supportsIncomingData(): Boolean = maxTransmitters != 0

private fun JvmMidiDevice.Info.signature(): String = "${name.orEmpty()}|${vendor.orEmpty()}|${description.orEmpty()}|${version.orEmpty()}"

private fun MidiMessage.payloadOrNull(): ByteArray? =
  if (length > 0) {
    runCatching { message.copyOfRange(0, length) }.getOrNull()
  } else {
    null
  }

private fun MidiDevice.ref(): MidiDeviceRef = MidiDeviceRef(id = id, name = name)
