package com.vireal.chordwizard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vireal.chordwizard.domain.model.Note
import com.vireal.chordwizard.domain.model.NoteWithOctave
import com.vireal.chordwizard.feature.pianorollui.PianoKeyboardView
import com.vireal.chordwizard.feature.pianorollui.PianoValidationMode
import com.vireal.chordwizard.feature.pianorollui.PitchMatchMode
import com.vireal.chordwizard.feature.pianorollui.PianoTrainingProgress
import com.vireal.chordwizard.feature.pianorollui.PianoTrainingSpec
import com.vireal.chordwizard.feature.pianorollui.PressedKeyUi
import com.vireal.chordwizard.feature.pianorollui.pianoKeyboardColors

private const val MIDI_C0_OFFSET = 12

@Composable
fun PianoKeyboard(
  pressedNotes: List<NoteWithOctave>,
  modifier: Modifier = Modifier,
  targetSequence: List<Int> = emptyList(),
  trainingProgress: PianoTrainingProgress? = null,
  validationMode: PianoValidationMode = PianoValidationMode.StrictSequence,
  pitchMatchMode: PitchMatchMode = PitchMatchMode.ExactMidi,
  showTargetDots: Boolean = true,
) {
  if (pressedNotes.isEmpty() && targetSequence.isEmpty()) return

  val pressedMidi = pressedNotes.map(::toMidi)
  val allRelevantMidi = pressedMidi + targetSequence
  val minRelevantMidi = allRelevantMidi.minOrNull() ?: return
  val maxRelevantMidi = allRelevantMidi.maxOrNull() ?: return
  val startOctave = octaveFromMidi(minRelevantMidi)
  val rangeStartMidi = toMidi(NoteWithOctave(Note.C, startOctave))
  val minimumEndMidi = toMidi(NoteWithOctave(Note.B, startOctave))
  val rangeEndMidi = maxOf(maxRelevantMidi, minimumEndMidi)
  val visibleRange = rangeStartMidi..rangeEndMidi

  val pressedKeys =
    pressedNotes.map { note ->
      PressedKeyUi(
        note = toMidi(note),
        velocity = 127,
        isTarget = false,
        isCorrect = false,
        startedAt = 0L,
      )
    }
  val trainingSpec =
    targetSequence.takeIf { it.isNotEmpty() }?.let { sequence ->
      PianoTrainingSpec(
        targetSequence = sequence,
        validationMode = validationMode,
        pitchMatchMode = pitchMatchMode,
      )
    }

  PianoKeyboardView(
    pressedKeys = pressedKeys,
    trainingSpec = trainingSpec,
    trainingProgress = trainingProgress,
    visibleRange = visibleRange,
    showTargetDots = showTargetDots,
    colors = pianoKeyboardColors(),
    modifier = modifier,
  )
}

private fun toMidi(note: NoteWithOctave): Int = note.absolutePosition + MIDI_C0_OFFSET

private fun octaveFromMidi(midi: Int): Int = ((midi - MIDI_C0_OFFSET) / 12).coerceAtLeast(0)
