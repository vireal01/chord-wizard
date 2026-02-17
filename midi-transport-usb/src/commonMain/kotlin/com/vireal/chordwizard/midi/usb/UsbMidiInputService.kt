package com.vireal.chordwizard.midi.usb

import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionConfig
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiDevice
import com.vireal.chordwizard.midi.core.MidiError
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.core.MidiMessageEvent
import com.vireal.chordwizard.midi.core.MidiPacket
import com.vireal.chordwizard.midi.core.MidiScanState
import com.vireal.chordwizard.midi.core.NoteEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

expect class PlatformUsbMidiInputService() : MidiInputService

fun createUsbMidiInputService(): MidiInputService = PlatformUsbMidiInputService()

enum class UsbMidiPlatform(val displayName: String) {
  ANDROID("Android"),
  IOS("iOS"),
  JVM_DESKTOP("JVM Desktop"),
  WEB_JS("Web JS"),
  WEB_WASM("Web Wasm"),
}

abstract class StubUsbMidiInputService(
  private val platform: UsbMidiPlatform,
) : MidiInputService {
  private val _availability =
    MutableStateFlow(
      MidiAvailability(
        status = MidiAvailability.Status.UNSUPPORTED,
        details = "USB MIDI is not implemented for ${platform.displayName} yet.",
      ),
    )
  private val _scanState = MutableStateFlow<MidiScanState>(MidiScanState.Idle)
  private val _discoveredDevices = MutableStateFlow<List<MidiDevice>>(emptyList())
  private val _connectionState = MutableStateFlow<MidiConnectionState>(MidiConnectionState.Disconnected)

  override val availability: StateFlow<MidiAvailability> = _availability
  override val scanState: StateFlow<MidiScanState> = _scanState
  override val discoveredDevices: StateFlow<List<MidiDevice>> = _discoveredDevices
  override val connectionState: StateFlow<MidiConnectionState> = _connectionState

  override val incomingPackets: Flow<MidiPacket> = emptyFlow()
  override val incomingMessages: Flow<MidiMessageEvent> = emptyFlow()
  override val noteEvents: Flow<NoteEvent> = emptyFlow()
  override val errors: Flow<MidiError> = emptyFlow()

  override suspend fun refreshAvailability() {
    notImplemented("refreshAvailability")
  }

  override suspend fun startScan() {
    notImplemented("startScan")
  }

  override suspend fun stopScan() {
    notImplemented("stopScan")
  }

  override suspend fun connect(deviceId: String, config: MidiConnectionConfig) {
    notImplemented("connect")
  }

  override suspend fun disconnect() {
    notImplemented("disconnect")
  }

  private fun notImplemented(operation: String): Nothing =
    error("USB MIDI is not implemented for ${platform.displayName} yet. Operation: $operation")
}
