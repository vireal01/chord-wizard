package com.vireal.chordwizard.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class InstrumentEngineFacadeTest {
  @Test
  fun `delegates to synth by default`() =
    runBlocking {
      val synth = FakeSynthEngine()
      val sampler = FakeSamplerEngine()
      val facade = InstrumentEngineFacade(synthEngine = synth, samplerEngine = sampler)

      facade.start()
      facade.noteOn(note = 60, velocity = 100)
      facade.noteOff(note = 60)
      facade.stop()

      assertEquals(1, synth.startCalls)
      assertEquals(1, synth.stopCalls)
      assertEquals(listOf(60), synth.noteOnNotes)
      assertEquals(listOf(60), synth.noteOffNotes)
      assertEquals(0, sampler.startCalls)
      assertEquals(0, sampler.noteOnNotes.size)
    }

  @Test
  fun `switches backend and delegates to sampler`() =
    runBlocking {
      val synth = FakeSynthEngine()
      val sampler = FakeSamplerEngine()
      val facade = InstrumentEngineFacade(synthEngine = synth, samplerEngine = sampler)

      facade.setBackend(InstrumentBackend.SAMPLER)
      facade.start()
      facade.noteOn(note = 64, velocity = 90)
      facade.noteOff(note = 64)

      assertEquals(1, synth.stopCalls)
      assertEquals(1, synth.allNotesOffCalls)
      assertEquals(1, sampler.startCalls)
      assertEquals(listOf(64), sampler.noteOnNotes)
      assertEquals(listOf(64), sampler.noteOffNotes)
    }

  private class FakeSynthEngine : SynthEngine {
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

    override suspend fun setWaveform(waveform: SynthWaveform) {
    }

    override suspend fun setMasterVolume(volume: Float) {
    }
  }

  private class FakeSamplerEngine : SamplerEngine {
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
}
