package com.vireal.chordwizard.bluetoothmidi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothMidiService {
  val availability: StateFlow<BluetoothMidiAvailability>
  val scanState: StateFlow<BluetoothMidiScanState>
  val discoveredDevices: StateFlow<List<BluetoothMidiDevice>>
  val connectionState: StateFlow<BluetoothMidiConnectionState>

  val incomingPackets: Flow<BluetoothMidiPacket>
  val incomingMessages: Flow<MidiMessageEvent>
  val noteEvents: Flow<NoteEvent>
  val errors: Flow<BluetoothMidiError>

  suspend fun refreshAvailability()
  suspend fun startScan()
  suspend fun stopScan()
  suspend fun connect(deviceId: String, config: BluetoothMidiConnectionConfig = BluetoothMidiConnectionConfig())
  suspend fun disconnect()
}

data class BluetoothMidiAvailability(
  val status: Status,
  val missingPermissions: Set<BluetoothMidiPermission> = emptySet(),
  val details: String? = null,
) {
  enum class Status {
    AVAILABLE,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    UNAVAILABLE,
  }
}

enum class BluetoothMidiPermission {
  BLUETOOTH,
  BLUETOOTH_SCAN,
  BLUETOOTH_CONNECT,
  LOCATION,
  MIDI_ACCESS,
  WEB_BLUETOOTH,
}

sealed interface BluetoothMidiScanState {
  data object Idle : BluetoothMidiScanState

  data class Scanning(
    val startedAtEpochMillis: Long,
  ) : BluetoothMidiScanState

  data class Stopped(
    val stoppedAtEpochMillis: Long,
    val reason: StopReason,
  ) : BluetoothMidiScanState {
    enum class StopReason {
      USER,
      SYSTEM,
      TIMEOUT,
    }
  }

  data class Failed(
    val error: BluetoothMidiError,
  ) : BluetoothMidiScanState
}

sealed interface BluetoothMidiConnectionState {
  data object Disconnected : BluetoothMidiConnectionState

  data class Connecting(
    val target: BluetoothMidiDeviceRef,
  ) : BluetoothMidiConnectionState

  data class Connected(
    val device: BluetoothMidiDevice,
  ) : BluetoothMidiConnectionState

  data class Disconnecting(
    val device: BluetoothMidiDeviceRef,
  ) : BluetoothMidiConnectionState

  data class Failed(
    val target: BluetoothMidiDeviceRef?,
    val error: BluetoothMidiError,
  ) : BluetoothMidiConnectionState
}

data class BluetoothMidiConnectionConfig(
  val autoReconnect: Boolean = false,
  val connectTimeoutMillis: Long = 15_000,
)
