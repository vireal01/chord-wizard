package com.vireal.chordwizard.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StubSamplerEngine : SamplerEngine {
  private val _state =
    MutableStateFlow(
      InstrumentEngineState(
        status = InstrumentEngineState.Status.STOPPED,
        details = "Sampler engine is not implemented yet.",
      ),
    )

  override val state: StateFlow<InstrumentEngineState> = _state

  override suspend fun start() {
    _state.value = _state.value.copy(status = InstrumentEngineState.Status.RUNNING)
  }

  override suspend fun stop() {
    _state.value = _state.value.copy(status = InstrumentEngineState.Status.STOPPED)
  }

  override suspend fun noteOn(
    note: Int,
    velocity: Int,
    channel: Int,
  ) {
  }

  override suspend fun noteOff(
    note: Int,
    channel: Int,
  ) {
  }

  override suspend fun allNotesOff() {
  }
}
