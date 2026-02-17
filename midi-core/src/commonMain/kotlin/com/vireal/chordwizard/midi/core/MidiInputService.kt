package com.vireal.chordwizard.midi.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MidiInputService {
  val availability: StateFlow<MidiAvailability>
  val scanState: StateFlow<MidiScanState>
  val discoveredDevices: StateFlow<List<MidiDevice>>
  val connectionState: StateFlow<MidiConnectionState>

  val incomingPackets: Flow<MidiPacket>
  val incomingMessages: Flow<MidiMessageEvent>
  val noteEvents: Flow<NoteEvent>
  val errors: Flow<MidiError>

  suspend fun refreshAvailability()
  suspend fun startScan()
  suspend fun stopScan()
  suspend fun connect(deviceId: String, config: MidiConnectionConfig = MidiConnectionConfig())
  suspend fun disconnect()
}

data class MidiAvailability(
  val status: Status,
  val details: String? = null,
) {
  enum class Status {
    AVAILABLE,
    UNSUPPORTED,
    UNAVAILABLE,
  }
}

sealed interface MidiScanState {
  data object Idle : MidiScanState

  data class Scanning(
    val startedAtEpochMillis: Long,
  ) : MidiScanState

  data class Stopped(
    val stoppedAtEpochMillis: Long,
    val reason: StopReason,
  ) : MidiScanState {
    enum class StopReason {
      USER,
      SYSTEM,
      TIMEOUT,
    }
  }

  data class Failed(
    val error: MidiError,
  ) : MidiScanState
}

sealed interface MidiConnectionState {
  data object Disconnected : MidiConnectionState

  data class Connecting(
    val target: MidiDeviceRef,
  ) : MidiConnectionState

  data class Connected(
    val device: MidiDevice,
  ) : MidiConnectionState

  data class Disconnecting(
    val device: MidiDeviceRef,
  ) : MidiConnectionState

  data class Failed(
    val target: MidiDeviceRef?,
    val error: MidiError,
  ) : MidiConnectionState
}

data class MidiConnectionConfig(
  val connectTimeoutMillis: Long = 15_000,
)
