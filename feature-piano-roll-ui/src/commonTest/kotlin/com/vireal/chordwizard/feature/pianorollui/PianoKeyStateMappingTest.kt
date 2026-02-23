package com.vireal.chordwizard.feature.pianorollui

import kotlin.test.Test
import kotlin.test.assertEquals

class PianoKeyStateMappingTest {
  @Test
  fun `maps pressed, target and wrong states`() {
    val visibleRange = 60..67
    val spec = PianoTrainingSpec(targetSequence = listOf(60, 64, 67))
    val progress =
      PianoTrainingProgress(
        nextExpectedIndex = 1,
        completedStepIndices = setOf(0),
        wrongActiveNotes = setOf(62),
        isCompleted = false,
      )
    val pressed =
      listOf(
        pressed(60),
        pressed(62),
        pressed(64),
      )

    val states =
      buildNoteStateByMidi(
        visibleRange = visibleRange,
        pressedKeys = pressed,
        trainingSpec = spec,
        trainingProgress = progress,
      )

    assertEquals(PianoKeyVisualState.CorrectPressed, states[60])
    assertEquals(PianoKeyVisualState.WrongPressed, states[62])
    assertEquals(PianoKeyVisualState.CorrectPressed, states[64])
    assertEquals(PianoKeyVisualState.TargetDot, states[67])
    assertEquals(PianoKeyVisualState.Idle, states[65])
  }

  @Test
  fun `no training mode keeps plain pressed state`() {
    val states =
      buildNoteStateByMidi(
        visibleRange = 60..62,
        pressedKeys = listOf(pressed(61)),
        trainingSpec = null,
        trainingProgress = null,
      )

    assertEquals(PianoKeyVisualState.Pressed, states[61])
    assertEquals(PianoKeyVisualState.Idle, states[60])
  }

  @Test
  fun `chord set pitch class marks pressed note in another octave as correct`() {
    val spec =
      PianoTrainingSpec(
        targetSequence = listOf(60, 64, 67),
        validationMode = PianoValidationMode.ChordSet,
        pitchMatchMode = PitchMatchMode.PitchClass,
      )
    val progress =
      PianoTrainingProgress(
        nextExpectedIndex = 1,
        completedStepIndices = setOf(0),
        wrongActiveNotes = emptySet(),
        isCompleted = false,
      )
    val states =
      buildNoteStateByMidi(
        visibleRange = 60..72,
        pressedKeys = listOf(pressed(72)),
        trainingSpec = spec,
        trainingProgress = progress,
      )

    assertEquals(PianoKeyVisualState.CorrectPressed, states[72])
    assertEquals(PianoKeyVisualState.TargetDot, states[60])
  }

  private fun pressed(note: Int): PressedKeyUi =
    PressedKeyUi(
      note = note,
      velocity = 90,
      isTarget = false,
      isCorrect = false,
      startedAt = 0L,
    )
}
