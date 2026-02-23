package com.vireal.chordwizard.feature.pianorollui

import com.vireal.chordwizard.midi.core.MidiDeviceRef
import com.vireal.chordwizard.midi.core.NoteEvent
import com.vireal.chordwizard.midi.core.NoteEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PianoTrainingEvaluatorTest {
  @Test
  fun `strict sequence completes for C4 E4 G4`() =
    runBlocking {
      val spec = PianoTrainingSpec(targetSequence = listOf(60, 64, 67))
      val progress =
        events(
          noteOn(60, 10),
          noteOff(60, 20),
          noteOn(64, 30),
          noteOff(64, 40),
          noteOn(67, 50),
        ).trackTrainingProgress(spec).last()

      assertEquals(3, progress.nextExpectedIndex)
      assertEquals(setOf(0, 1, 2), progress.completedStepIndices)
      assertTrue(progress.isCompleted)
      assertTrue(progress.wrongActiveNotes.isEmpty())
    }

  @Test
  fun `wrong note is tracked and removed on note off`() =
    runBlocking {
      val spec = PianoTrainingSpec(targetSequence = listOf(60, 64, 67))
      val progresses =
        events(
          noteOn(62, 10),
          noteOff(62, 20),
        ).trackTrainingProgress(spec).toList()

      val afterWrongPress = progresses[1]
      val afterWrongRelease = progresses[2]

      assertEquals(setOf(62), afterWrongPress.wrongActiveNotes)
      assertTrue(afterWrongRelease.wrongActiveNotes.isEmpty())
    }

  @Test
  fun `repeated note on completed step does not move progress and is not wrong`() =
    runBlocking {
      val spec = PianoTrainingSpec(targetSequence = listOf(60, 64, 67))
      val progresses =
        events(
          noteOn(60, 10),
          noteOn(60, 11),
        ).trackTrainingProgress(spec).toList()

      val afterRepeated = progresses[2]
      assertEquals(1, afterRepeated.nextExpectedIndex)
      assertEquals(setOf(0), afterRepeated.completedStepIndices)
      assertFalse(60 in afterRepeated.wrongActiveNotes)
    }

  @Test
  fun `empty sequence behaves as no-op`() =
    runBlocking {
      val spec = PianoTrainingSpec(targetSequence = emptyList())
      val progresses =
        events(
          noteOn(60, 10),
          noteOff(60, 20),
        ).trackTrainingProgress(spec).toList()

      progresses.forEach { state ->
        assertEquals(0, state.nextExpectedIndex)
        assertTrue(state.completedStepIndices.isEmpty())
        assertTrue(state.wrongActiveNotes.isEmpty())
        assertFalse(state.isCompleted)
      }
    }

  private fun events(vararg values: NoteEvent): Flow<NoteEvent> = if (values.isEmpty()) emptyFlow() else flowOf(*values)

  private fun noteOn(note: Int, at: Long): NoteEvent =
    NoteEvent(
      device = MidiDeviceRef(id = "dev-1", name = "Test Device"),
      channel = 0,
      note = note,
      velocity = 100,
      type = NoteEventType.NOTE_ON,
      receivedAtEpochMillis = at,
    )

  private fun noteOff(note: Int, at: Long): NoteEvent =
    NoteEvent(
      device = MidiDeviceRef(id = "dev-1", name = "Test Device"),
      channel = 0,
      note = note,
      velocity = 0,
      type = NoteEventType.NOTE_OFF,
      receivedAtEpochMillis = at,
    )
}
