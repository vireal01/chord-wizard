package com.vireal.chordwizard.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PcmEncodingTest {
  @Test
  fun `float mono samples are encoded as little endian pcm16 with clamping`() {
    val samples = floatArrayOf(-1.5f, -1f, -0.5f, 0f, 0.5f, 1f, 1.5f)

    val bytes = floatMonoToPcm16Le(samples)

    assertEquals(samples.size * 2, bytes.size)
    assertContentEquals(
      intArrayOf(-32767, -32767, -16383, 0, 16383, 32767, 32767),
      decodePcm16Le(bytes),
    )
  }

  @Test
  fun `empty samples produce empty byte array`() {
    assertContentEquals(ByteArray(0), floatMonoToPcm16Le(floatArrayOf()))
  }

  private fun decodePcm16Le(bytes: ByteArray): IntArray {
    val out = IntArray(bytes.size / 2)
    var index = 0
    var byteIndex = 0
    while (byteIndex < bytes.size) {
      val lo = bytes[byteIndex++].toInt() and 0xFF
      val hi = bytes[byteIndex++].toInt()
      out[index++] = (hi shl 8) or lo
    }
    return out
  }
}
