package com.vireal.chordwizard.audio

import kotlinx.coroutines.flow.StateFlow

interface InstrumentEngine {
  val state: StateFlow<InstrumentEngineState>

  suspend fun start()

  suspend fun stop()

  suspend fun noteOn(
    note: Int,
    velocity: Int,
    channel: Int = DEFAULT_MIDI_CHANNEL,
  )

  suspend fun noteOff(
    note: Int,
    channel: Int = DEFAULT_MIDI_CHANNEL,
  )

  suspend fun allNotesOff()

  companion object {
    const val DEFAULT_MIDI_CHANNEL: Int = 0
  }
}

data class InstrumentEngineState(
  val status: Status,
  val details: String? = null,
) {
  enum class Status {
    STOPPED,
    STARTING,
    RUNNING,
    FAILED,
  }
}
