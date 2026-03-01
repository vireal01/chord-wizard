package com.vireal.chordwizard.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SynthEngineImplTest {
  @Test
  fun `note on allocates voices up to polyphony limit`() =
    runTest {
      val engine =
        SynthEngineImpl(
          audioOutput = RecordingAudioOutput(),
          config =
            SynthEngineImpl.SynthEngineConfig(
              maxVoices = 16,
              blockSize = 64,
            ),
        )

      engine.start()
      repeat(20) { i ->
        engine.noteOn(note = 60 + i, velocity = 100, channel = 0)
      }

      assertEquals(16, engine.debugActiveVoiceCount())
      engine.stop()
    }

  @Test
  fun `all notes off releases active voices`() =
    runTest {
      val output = RecordingAudioOutput()
      val engine =
        SynthEngineImpl(
          audioOutput = output,
          config =
            SynthEngineImpl.SynthEngineConfig(
              blockSize = 128,
              releaseSeconds = 0.01f,
            ),
        )

      engine.start()
      engine.noteOn(note = 60, velocity = 100, channel = 0)
      val blockWithNote = engine.debugRenderBlockForTest(256)
      assertTrue(blockWithNote.any { kotlin.math.abs(it) > 0.0001f })

      engine.allNotesOff()
      repeat(20) {
        engine.debugRenderBlockForTest(256)
      }

      assertEquals(0, engine.debugActiveVoiceCount())
      engine.stop()
    }

  @Test
  fun `master volume zero produces silence`() =
    runTest {
      val engine =
        SynthEngineImpl(
          audioOutput = RecordingAudioOutput(),
          config = SynthEngineImpl.SynthEngineConfig(blockSize = 128),
        )

      engine.start()
      engine.setMasterVolume(0f)
      engine.noteOn(note = 64, velocity = 127, channel = 0)
      val block = engine.debugRenderBlockForTest(256)

      assertTrue(block.all { kotlin.math.abs(it) <= 0.0001f })
      engine.stop()
    }

  private class RecordingAudioOutput : AudioOutput {
    val started = MutableStateFlow(false)

    override suspend fun start(sampleRateHz: Int, channels: Int) {
      started.value = true
    }

    override suspend fun writeMonoPcm(samples: FloatArray) {
    }

    override suspend fun stop() {
      started.value = false
    }
  }
}
