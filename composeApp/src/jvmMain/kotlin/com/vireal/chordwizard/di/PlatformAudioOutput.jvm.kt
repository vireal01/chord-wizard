package com.vireal.chordwizard.di

import com.vireal.chordwizard.audio.AudioOutput
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

actual fun createPlatformAudioOutput(): AudioOutput = JvmSourceLineAudioOutput()

private class JvmSourceLineAudioOutput : AudioOutput {
  private val lock = Any()
  private var line: SourceDataLine? = null

  override val blocksOnWrite: Boolean = true

  override suspend fun start(
    sampleRateHz: Int,
    channels: Int,
  ) {
    synchronized(lock) {
      if (line != null) return

      val format = AudioFormat(sampleRateHz.toFloat(), BIT_DEPTH, channels, true, false)
      val info = DataLine.Info(SourceDataLine::class.java, format)
      val sourceLine = AudioSystem.getLine(info) as SourceDataLine
      val requestedBufferBytes = calculateRequestedBufferBytes(sampleRateHz = sampleRateHz, channels = channels)
      sourceLine.open(format, requestedBufferBytes)
      sourceLine.start()
      line = sourceLine
    }
  }

  override suspend fun writeMonoPcm(samples: FloatArray) {
    val activeLine = synchronized(lock) { line } ?: return
    if (samples.isEmpty()) return

    val bytes = ByteArray(samples.size * BYTES_PER_SAMPLE)
    var byteIndex = 0

    samples.forEach { sample ->
      val pcm16 = (sample.coerceIn(-1f, 1f) * MAX_PCM_AMPLITUDE).toInt().coerceIn(MIN_PCM, MAX_PCM)
      bytes[byteIndex++] = (pcm16 and BYTE_MASK).toByte()
      bytes[byteIndex++] = ((pcm16 ushr BYTE_SHIFT) and BYTE_MASK).toByte()
    }

    var offset = 0
    while (offset < bytes.size) {
      val written = activeLine.write(bytes, offset, bytes.size - offset)
      if (written <= 0) break
      offset += written
    }
  }

  override suspend fun stop() {
    synchronized(lock) {
      val activeLine = line ?: return
      line = null

      runCatching { activeLine.drain() }
      runCatching { activeLine.stop() }
      runCatching { activeLine.close() }
    }
  }

  private companion object {
    const val TARGET_OUTPUT_BUFFER_MS = 24

    const val BIT_DEPTH = 16
    const val BYTES_PER_SAMPLE = 2

    const val MAX_PCM_AMPLITUDE = 32767f
    const val MAX_PCM = 32767
    const val MIN_PCM = -32768

    const val BYTE_MASK = 0xFF
    const val BYTE_SHIFT = 8

    fun calculateRequestedBufferBytes(
      sampleRateHz: Int,
      channels: Int,
    ): Int {
      val bytesPerFrame = channels * BYTES_PER_SAMPLE
      val targetFrames = (sampleRateHz * TARGET_OUTPUT_BUFFER_MS) / 1000
      val minFrames = 256
      return maxOf(targetFrames, minFrames) * bytesPerFrame
    }
  }
}
