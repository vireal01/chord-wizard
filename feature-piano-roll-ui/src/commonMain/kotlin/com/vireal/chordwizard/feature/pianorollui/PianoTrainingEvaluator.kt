package com.vireal.chordwizard.feature.pianorollui

import com.vireal.chordwizard.midi.core.NoteEvent
import com.vireal.chordwizard.midi.core.NoteEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold

internal fun Flow<NoteEvent>.trackTrainingProgress(spec: PianoTrainingSpec): Flow<PianoTrainingProgress> =
  runningFold(TrainingAccumulator(spec)) { acc, event ->
    acc.apply(event)
  }.map { acc ->
    acc.snapshot()
  }

internal fun buildNoteStateByMidi(
  visibleRange: IntRange,
  pressedKeys: List<PressedKeyUi>,
  trainingSpec: PianoTrainingSpec?,
  trainingProgress: PianoTrainingProgress?,
): Map<Int, PianoKeyVisualState> {
  val pressedByNote = pressedKeys.associateBy { it.note }
  val hasTraining = trainingSpec != null && trainingSpec.targetSequence.isNotEmpty()
  val targetSequence = trainingSpec?.targetSequence.orEmpty()
  val nextExpectedIndex = trainingProgress?.nextExpectedIndex ?: 0
  val expectedNote = targetSequence.getOrNull(nextExpectedIndex)
  val wrongNotes = trainingProgress?.wrongActiveNotes.orEmpty()
  val completedNotes =
    trainingProgress
      ?.completedStepIndices
      .orEmpty()
      .mapNotNull(targetSequence::getOrNull)
      .toSet()

  return buildMap {
    for (note in visibleRange) {
      val isPressed = pressedByNote.containsKey(note)
      val isTarget = note in targetSequence
      val isWrong = hasTraining && isPressed && note in wrongNotes
      val isCorrectPressed = hasTraining && isPressed && (note == expectedNote || note in completedNotes)
      val state =
        when {
          isWrong -> PianoKeyVisualState.WrongPressed
          isCorrectPressed -> PianoKeyVisualState.CorrectPressed
          isTarget -> PianoKeyVisualState.TargetDot
          isPressed -> PianoKeyVisualState.Pressed
          else -> PianoKeyVisualState.Idle
        }

      put(note, state)
    }
  }
}

private class TrainingAccumulator(
  private val spec: PianoTrainingSpec,
) {
  private var nextExpectedIndex: Int = 0
  private val completedStepIndices = linkedSetOf<Int>()
  private val wrongActiveNotes = linkedSetOf<Int>()

  fun apply(event: NoteEvent): TrainingAccumulator {
    if (spec.targetSequence.isEmpty()) return this

    when (spec.validationMode) {
      PianoValidationMode.StrictSequence -> applyStrictSequence(event)
    }

    return this
  }

  fun snapshot(): PianoTrainingProgress {
    if (spec.targetSequence.isEmpty()) {
      return PianoTrainingProgress(
        nextExpectedIndex = 0,
        completedStepIndices = emptySet(),
        wrongActiveNotes = emptySet(),
        isCompleted = false,
      )
    }

    return PianoTrainingProgress(
      nextExpectedIndex = nextExpectedIndex,
      completedStepIndices = completedStepIndices.toSet(),
      wrongActiveNotes = wrongActiveNotes.toSet(),
      isCompleted = nextExpectedIndex >= spec.targetSequence.size,
    )
  }

  private fun applyStrictSequence(event: NoteEvent) {
    val isNoteOff = event.type == NoteEventType.NOTE_OFF || (event.type == NoteEventType.NOTE_ON && event.velocity == 0)
    if (isNoteOff) {
      wrongActiveNotes.remove(event.note)
      return
    }

    val expectedIndex = nextExpectedIndex
    val expectedNote = spec.targetSequence.getOrNull(expectedIndex) ?: return

    if (event.note == expectedNote) {
      completedStepIndices.add(expectedIndex)
      nextExpectedIndex = expectedIndex + 1
      wrongActiveNotes.remove(event.note)
      return
    }

    val isAlreadyCompletedNote = completedStepIndices.any { idx -> spec.targetSequence.getOrNull(idx) == event.note }
    if (!isAlreadyCompletedNote) {
      wrongActiveNotes.add(event.note)
    }
  }
}
