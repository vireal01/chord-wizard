package com.vireal.chordwizard.audio

interface AudioOutput {
  val blocksOnWrite: Boolean
    get() = false

  suspend fun start(
    sampleRateHz: Int,
    channels: Int,
  )

  suspend fun writeMonoPcm(samples: FloatArray)

  suspend fun stop()
}

class NoOpAudioOutput : AudioOutput {
  override suspend fun start(sampleRateHz: Int, channels: Int) {
  }

  override suspend fun writeMonoPcm(samples: FloatArray) {
  }

  override suspend fun stop() {
  }
}
