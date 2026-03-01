package com.vireal.chordwizard.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class InstrumentBackend {
  SYNTH,
  SAMPLER,
}

class InstrumentEngineFacade(
  private val synthEngine: SynthEngine,
  private val samplerEngine: SamplerEngine,
  initialBackend: InstrumentBackend = InstrumentBackend.SYNTH,
) : InstrumentEngine {
  private var backend: InstrumentBackend = initialBackend
  private val _state = MutableStateFlow(currentEngine().state.value)

  override val state: StateFlow<InstrumentEngineState> = _state

  suspend fun setBackend(newBackend: InstrumentBackend) {
    if (backend == newBackend) return
    currentEngine().allNotesOff()
    currentEngine().stop()
    backend = newBackend
    _state.value = currentEngine().state.value
  }

  override suspend fun start() {
    currentEngine().start()
    _state.value = currentEngine().state.value
  }

  override suspend fun stop() {
    currentEngine().stop()
    _state.value = currentEngine().state.value
  }

  override suspend fun noteOn(
    note: Int,
    velocity: Int,
    channel: Int,
  ) {
    currentEngine().noteOn(note = note, velocity = velocity, channel = channel)
  }

  override suspend fun noteOff(
    note: Int,
    channel: Int,
  ) {
    currentEngine().noteOff(note = note, channel = channel)
  }

  override suspend fun allNotesOff() {
    currentEngine().allNotesOff()
  }

  private fun currentEngine(): InstrumentEngine =
    when (backend) {
      InstrumentBackend.SYNTH -> synthEngine
      InstrumentBackend.SAMPLER -> samplerEngine
    }
}
