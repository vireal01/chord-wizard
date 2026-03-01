package com.vireal.chordwizard.audio

interface SynthEngine : InstrumentEngine {
  suspend fun setWaveform(waveform: SynthWaveform)

  suspend fun setMasterVolume(volume: Float)
}

enum class SynthWaveform {
  SINE,
  TRIANGLE,
  SQUARE,
  SAW,
}
