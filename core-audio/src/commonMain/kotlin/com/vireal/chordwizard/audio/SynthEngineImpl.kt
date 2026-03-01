package com.vireal.chordwizard.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SynthEngineImpl : SynthEngine {
  private val _state = MutableStateFlow(InstrumentEngineState(InstrumentEngineState.Status.STOPPED))

  override val state: StateFlow<InstrumentEngineState> = _state

  private var waveform: SynthWaveform = SynthWaveform.SINE
  private var masterVolume: Float = 1f

  override suspend fun start() {
    _state.value = InstrumentEngineState(InstrumentEngineState.Status.RUNNING)
  }

  override suspend fun stop() {
    _state.value = InstrumentEngineState(InstrumentEngineState.Status.STOPPED)
  }

  override suspend fun noteOn(
    note: Int,
    velocity: Int,
    channel: Int,
  ) {
    // Stub implementation for future real DSP backend.
    if (_state.value.status != InstrumentEngineState.Status.RUNNING) return
    if (note !in 0..127 || velocity !in 0..127 || channel !in 0..15) return
    if (masterVolume <= 0f) return
    if (waveform !in SynthWaveform.entries) return
  }

  override suspend fun noteOff(
    note: Int,
    channel: Int,
  ) {
    if (_state.value.status != InstrumentEngineState.Status.RUNNING) return
    if (note !in 0..127 || channel !in 0..15) return
  }

  override suspend fun allNotesOff() {
    if (_state.value.status != InstrumentEngineState.Status.RUNNING) return
  }

  override suspend fun setWaveform(waveform: SynthWaveform) {
    this.waveform = waveform
  }

  override suspend fun setMasterVolume(volume: Float) {
    masterVolume = volume.coerceIn(0f, 1f)
  }
}
