package com.vireal.chordwizard.audio

fun floatMonoToPcm16Le(samples: FloatArray): ByteArray {
  if (samples.isEmpty()) return ByteArray(0)

  val out = ByteArray(samples.size * BYTES_PER_SAMPLE)
  var byteIndex = 0

  samples.forEach { sample ->
    val pcm16 = (sample.coerceIn(-1f, 1f) * MAX_PCM_AMPLITUDE).toInt().coerceIn(MIN_PCM, MAX_PCM)
    out[byteIndex++] = (pcm16 and BYTE_MASK).toByte()
    out[byteIndex++] = ((pcm16 ushr BYTE_SHIFT) and BYTE_MASK).toByte()
  }

  return out
}

private const val BYTES_PER_SAMPLE = 2
private const val MAX_PCM_AMPLITUDE = 32767f
private const val MAX_PCM = 32767
private const val MIN_PCM = -32768
private const val BYTE_MASK = 0xFF
private const val BYTE_SHIFT = 8
