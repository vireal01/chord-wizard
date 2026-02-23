package com.vireal.chordwizard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vireal.chordwizard.domain.model.Note
import com.vireal.chordwizard.domain.model.NoteWithOctave
import com.vireal.chordwizard.feature.pianorollui.PianoKeyboardView
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
) {
  if (pressedNotes.isEmpty() && targetSequence.isEmpty()) return

  val visibleRange =
    if (pressedNotes.isNotEmpty()) {
      val startOctave = pressedNotes.minOf { it.octave }
      val lastNote = pressedNotes.maxBy { it.absolutePosition }
      val minimumEnd = NoteWithOctave(Note.B, startOctave)
      val actualEnd =
        if (lastNote.absolutePosition < minimumEnd.absolutePosition) {
          minimumEnd
        } else {
          lastNote
        }

      toMidi(NoteWithOctave(Note.C, startOctave))..toMidi(actualEnd)
    } else {
      val minTargetMidi = targetSequence.minOrNull() ?: return
      val maxTargetMidi = targetSequence.maxOrNull() ?: return
      val startOctave = octaveFromMidi(minTargetMidi)
      val minimumEndMidi = toMidi(NoteWithOctave(Note.B, startOctave))
      val actualEndMidi = maxOf(maxTargetMidi, minimumEndMidi)
      toMidi(NoteWithOctave(Note.C, startOctave))..actualEndMidi
    }

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
  val trainingSpec = targetSequence.takeIf { it.isNotEmpty() }?.let(::PianoTrainingSpec)

  PianoKeyboardView(
    pressedKeys = pressedKeys,
    trainingSpec = trainingSpec,
    trainingProgress = trainingProgress,
    visibleRange = visibleRange,
    colors = pianoKeyboardColors(),
    modifier = modifier,
  )
}

private fun toMidi(note: NoteWithOctave): Int = note.absolutePosition + MIDI_C0_OFFSET

private fun octaveFromMidi(midi: Int): Int = ((midi - MIDI_C0_OFFSET) / 12).coerceAtLeast(0)
