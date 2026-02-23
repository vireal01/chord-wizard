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
  val pitchMatchMode = trainingSpec?.pitchMatchMode ?: PitchMatchMode.ExactMidi
  val isChordSet = trainingSpec?.validationMode == PianoValidationMode.ChordSet
  val nextExpectedIndex = trainingProgress?.nextExpectedIndex ?: 0
  val expectedNote = targetSequence.getOrNull(nextExpectedIndex)
  val wrongNotes = trainingProgress?.wrongActiveNotes.orEmpty()
  val completedNotes =
    trainingProgress
      ?.completedStepIndices
      .orEmpty()
      .mapNotNull(targetSequence::getOrNull)
      .toSet()
  val completedMatchKeys = completedNotes.map { normalizeMidiNote(it, pitchMatchMode) }.toSet()

  return buildMap {
    for (note in visibleRange) {
      val isPressed = pressedByNote.containsKey(note)
      val isTarget = note in targetSequence
      val isWrong = hasTraining && isPressed && note in wrongNotes
      val isCorrectPressed =
        when {
          !hasTraining || !isPressed -> false
          isChordSet -> normalizeMidiNote(note, pitchMatchMode) in completedMatchKeys
          else -> note == expectedNote || note in completedNotes
        }
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
  private val targetStepByMatchKey = buildTargetStepByMatchKey(spec)
  private val requiredStepIndices = targetStepByMatchKey.values.toSet()
  private val activeRawNoteCounts = linkedMapOf<Int, Int>()
  private val activeMatchKeyCounts = linkedMapOf<Int, Int>()

  fun apply(event: NoteEvent): TrainingAccumulator {
    if (spec.targetSequence.isEmpty()) return this

    when (spec.validationMode) {
      PianoValidationMode.StrictSequence -> applyStrictSequence(event)
      PianoValidationMode.ChordSet -> applyChordSet(event)
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
      isCompleted =
        when (spec.validationMode) {
          PianoValidationMode.StrictSequence -> nextExpectedIndex >= spec.targetSequence.size
          PianoValidationMode.ChordSet -> requiredStepIndices.isNotEmpty() && completedStepIndices.containsAll(requiredStepIndices) && wrongActiveNotes.isEmpty()
        },
    )
  }

  private fun applyStrictSequence(event: NoteEvent) {
    val isNoteOff = isNoteOff(event)
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

  private fun applyChordSet(event: NoteEvent) {
    val matchKey = toMatchKey(event.note)

    if (isNoteOff(event)) {
      decrementCount(activeRawNoteCounts, event.note)
      decrementCount(activeMatchKeyCounts, matchKey)
    } else {
      incrementCount(activeRawNoteCounts, event.note)
      incrementCount(activeMatchKeyCounts, matchKey)
    }

    recomputeChordSetState()
  }

  private fun recomputeChordSetState() {
    completedStepIndices.clear()
    targetStepByMatchKey.forEach { (matchKey, stepIndex) ->
      if ((activeMatchKeyCounts[matchKey] ?: 0) > 0) {
        completedStepIndices.add(stepIndex)
      }
    }

    wrongActiveNotes.clear()
    activeRawNoteCounts.keys.forEach { rawNote ->
      if (toMatchKey(rawNote) !in targetStepByMatchKey.keys) {
        wrongActiveNotes.add(rawNote)
      }
    }

    nextExpectedIndex = completedStepIndices.size
  }

  private fun toMatchKey(note: Int): Int = normalizeMidiNote(note, spec.pitchMatchMode)
}

private fun buildTargetStepByMatchKey(spec: PianoTrainingSpec): LinkedHashMap<Int, Int> {
  val byMatchKey = linkedMapOf<Int, Int>()
  spec.targetSequence.forEachIndexed { index, note ->
    val matchKey = normalizeMidiNote(note, spec.pitchMatchMode)
    if (matchKey !in byMatchKey) {
      byMatchKey[matchKey] = index
    }
  }
  return byMatchKey
}

private fun normalizeMidiNote(
  note: Int,
  mode: PitchMatchMode,
): Int =
  when (mode) {
    PitchMatchMode.ExactMidi -> note
    PitchMatchMode.PitchClass -> ((note % 12) + 12) % 12
  }

private fun isNoteOff(event: NoteEvent): Boolean =
  event.type == NoteEventType.NOTE_OFF || (event.type == NoteEventType.NOTE_ON && event.velocity == 0)

private fun incrementCount(
  map: MutableMap<Int, Int>,
  key: Int,
) {
  map[key] = (map[key] ?: 0) + 1
}

private fun decrementCount(
  map: MutableMap<Int, Int>,
  key: Int,
) {
  val current = map[key] ?: return
  if (current <= 1) {
    map.remove(key)
  } else {
    map[key] = current - 1
  }
}
