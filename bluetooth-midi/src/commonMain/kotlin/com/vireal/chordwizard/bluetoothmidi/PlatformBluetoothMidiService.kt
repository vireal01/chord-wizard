package com.vireal.chordwizard.bluetoothmidi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

expect class PlatformBluetoothMidiService() : BluetoothMidiService

fun createBluetoothMidiService(): BluetoothMidiService = PlatformBluetoothMidiService()

enum class BluetoothMidiPlatform(val displayName: String) {
  ANDROID("Android"),
  IOS("iOS"),
  JVM_DESKTOP("JVM Desktop"),
  WEB_JS("Web JS"),
  WEB_WASM("Web Wasm"),
}

abstract class StubBluetoothMidiService(
  private val platform: BluetoothMidiPlatform,
) : BluetoothMidiService {
  private val _availability =
    MutableStateFlow(
      BluetoothMidiAvailability(
        status = BluetoothMidiAvailability.Status.UNSUPPORTED,
        details = "Bluetooth MIDI is not implemented for ${platform.displayName} yet.",
      ),
    )
  private val _scanState = MutableStateFlow<BluetoothMidiScanState>(BluetoothMidiScanState.Idle)
  private val _discoveredDevices = MutableStateFlow(emptyList<BluetoothMidiDevice>())
  private val _connectionState =
    MutableStateFlow<BluetoothMidiConnectionState>(BluetoothMidiConnectionState.Disconnected)

  override val availability: StateFlow<BluetoothMidiAvailability> = _availability
  override val scanState: StateFlow<BluetoothMidiScanState> = _scanState
  override val discoveredDevices: StateFlow<List<BluetoothMidiDevice>> = _discoveredDevices
  override val connectionState: StateFlow<BluetoothMidiConnectionState> = _connectionState

  override val incomingPackets: Flow<BluetoothMidiPacket> = emptyFlow()
  override val incomingMessages: Flow<MidiMessageEvent> = emptyFlow()
  override val noteEvents: Flow<NoteEvent> = emptyFlow()
  override val errors: Flow<BluetoothMidiError> = emptyFlow()

  override suspend fun refreshAvailability() {
    notImplemented("refreshAvailability")
  }

  override suspend fun startScan() {
    notImplemented("startScan")
  }

  override suspend fun stopScan() {
    notImplemented("stopScan")
  }

  override suspend fun connect(deviceId: String, config: BluetoothMidiConnectionConfig) {
    notImplemented("connect")
  }

  override suspend fun disconnect() {
    notImplemented("disconnect")
  }

  private fun notImplemented(operation: String): Nothing =
    error(
      "Bluetooth MIDI is not implemented for ${platform.displayName} yet. Operation: $operation",
    )
}
