package com.vireal.chordwizard.audio

import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionConfig
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiDevice
import com.vireal.chordwizard.midi.core.MidiDeviceRef
import com.vireal.chordwizard.midi.core.MidiError
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.core.MidiMessageEvent
import com.vireal.chordwizard.midi.core.MidiPacket
import com.vireal.chordwizard.midi.core.MidiScanState
import com.vireal.chordwizard.midi.core.NoteEvent
import com.vireal.chordwizard.midi.core.NoteEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class MidiAudioBridgeTest {
  @Test
  fun `attach on enabled route starts engine and maps note events`() =
    runTest {
      val midiInput = FakeMidiInputService()
      val engine = FakeInstrumentEngine()
      val bridge =
        MidiAudioBridge(
          midiInputService = midiInput,
          instrumentEngine = engine,
          playbackPolicy = AudioPlaybackPolicy(setOf(AudioRouteKey.NOTE_VISUALIZER)),
        )

      bridge.attach(AudioRouteKey.NOTE_VISUALIZER)
      midiInput.emit(NoteEventType.NOTE_ON, note = 60, velocity = 100)
      midiInput.emit(NoteEventType.NOTE_ON, note = 61, velocity = 0)
      midiInput.emit(NoteEventType.NOTE_OFF, note = 60, velocity = 0)
      awaitBridgeWork()

      assertEquals(1, engine.startCalls)
      assertEquals(listOf(60), engine.noteOnNotes)
      assertEquals(listOf(61, 60), engine.noteOffNotes)

      bridge.detach(AudioRouteKey.NOTE_VISUALIZER)
      awaitBridgeWork()
      assertEquals(1, engine.allNotesOffCalls)
      assertEquals(1, engine.stopCalls)
      bridge.dispose()
    }

  @Test
  fun `attach on disabled route does not start engine`() =
    runTest {
      val midiInput = FakeMidiInputService()
      val engine = FakeInstrumentEngine()
      val bridge =
        MidiAudioBridge(
          midiInputService = midiInput,
          instrumentEngine = engine,
          playbackPolicy = AudioPlaybackPolicy(setOf(AudioRouteKey.NOTE_VISUALIZER)),
        )

      bridge.attach(AudioRouteKey.HOME)
      midiInput.emit(NoteEventType.NOTE_ON, note = 60, velocity = 100)
      awaitBridgeWork()

      assertEquals(0, engine.startCalls)
      assertEquals(0, engine.noteOnNotes.size)
      bridge.dispose()
    }

  private suspend fun awaitBridgeWork() {
    withContext(Dispatchers.Default) {
      delay(50)
    }
  }

  private class FakeInstrumentEngine : InstrumentEngine {
    private val _state = MutableStateFlow(InstrumentEngineState(InstrumentEngineState.Status.STOPPED))
    override val state: StateFlow<InstrumentEngineState> = _state

    var startCalls: Int = 0
    var stopCalls: Int = 0
    var allNotesOffCalls: Int = 0
    val noteOnNotes = mutableListOf<Int>()
    val noteOffNotes = mutableListOf<Int>()

    override suspend fun start() {
      startCalls += 1
      _state.value = InstrumentEngineState(InstrumentEngineState.Status.RUNNING)
    }

    override suspend fun stop() {
      stopCalls += 1
      _state.value = InstrumentEngineState(InstrumentEngineState.Status.STOPPED)
    }

    override suspend fun noteOn(note: Int, velocity: Int, channel: Int) {
      noteOnNotes += note
    }

    override suspend fun noteOff(note: Int, channel: Int) {
      noteOffNotes += note
    }

    override suspend fun allNotesOff() {
      allNotesOffCalls += 1
    }
  }

  private class FakeMidiInputService : MidiInputService {
    private val notes = MutableSharedFlow<NoteEvent>(replay = 16, extraBufferCapacity = 16)

    override val availability: StateFlow<MidiAvailability> =
      MutableStateFlow(MidiAvailability(status = MidiAvailability.Status.AVAILABLE))
    override val scanState: StateFlow<MidiScanState> = MutableStateFlow(MidiScanState.Idle)
    override val discoveredDevices: StateFlow<List<MidiDevice>> = MutableStateFlow(emptyList())
    override val connectionState: StateFlow<MidiConnectionState> = MutableStateFlow(MidiConnectionState.Disconnected)
    override val incomingPackets: Flow<MidiPacket> = emptyFlow()
    override val incomingMessages: Flow<MidiMessageEvent> = emptyFlow()
    override val noteEvents: Flow<NoteEvent> = notes
    override val errors: Flow<MidiError> = flowOf()

    override suspend fun refreshAvailability() {
    }

    override suspend fun startScan() {
    }

    override suspend fun stopScan() {
    }

    override suspend fun connect(deviceId: String, config: MidiConnectionConfig) {
    }

    override suspend fun disconnect() {
    }

    suspend fun emit(type: NoteEventType, note: Int, velocity: Int) {
      notes.emit(
        NoteEvent(
          device = MidiDeviceRef(id = "dev1", name = "Test Device"),
          channel = 0,
          note = note,
          velocity = velocity,
          type = type,
          receivedAtEpochMillis = 0L,
        ),
      )
    }
  }
}
