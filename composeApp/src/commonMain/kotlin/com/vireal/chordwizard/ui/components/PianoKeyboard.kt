package com.vireal.chordwizard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.vireal.chordwizard.domain.model.Note
import com.vireal.chordwizard.domain.model.NoteWithOctave

/**
 * Piano keyboard component that highlights pressed notes
 * Displays from start octave up to the highest note in the chord
 * Minimum: Always shows at least 1 full octave (7 white keys)
 */
@Composable
fun PianoKeyboard(
  pressedNotes: List<NoteWithOctave>,
  modifier: Modifier = Modifier,
) {
  if (pressedNotes.isEmpty()) return

  val whiteKeyColor = MaterialTheme.colorScheme.surface
  val blackKeyColor = MaterialTheme.colorScheme.onSurface
  val pressedColor = MaterialTheme.colorScheme.primary
  val borderColor = MaterialTheme.colorScheme.outline

  // Calculate range: from lowest to highest note
  val startOctave = pressedNotes.minOf { it.octave }
  val endOctave = pressedNotes.maxOf { it.octave }
  val lastNote = pressedNotes.maxBy { it.absolutePosition }

  // Calculate minimum end (at least 1 full octave from start)
  val minimumEnd = NoteWithOctave(Note.B, startOctave)
  val actualEnd =
    if (lastNote.absolutePosition < minimumEnd.absolutePosition) {
      minimumEnd // Extend to show full octave
    } else {
      lastNote // Use actual last note if it's beyond first octave
    }

  // Calculate how many white keys to display
  val whiteKeysCount = calculateWhiteKeysCount(startOctave, actualEnd.octave, actualEnd)

  Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
    val whiteKeyWidth = size.width / whiteKeysCount
    val whiteKeyHeight = size.height
    val blackKeyWidth = whiteKeyWidth * 0.6f
    val blackKeyHeight = whiteKeyHeight * 0.6f

    // All white keys in chromatic scale
    val whiteKeys =
      listOf(
        Note.C,
        Note.D,
        Note.E,
        Note.F,
        Note.G,
        Note.A,
        Note.B,
      )

    // Draw white keys
    var whiteKeyIndex = 0
    for (octave in startOctave..actualEnd.octave) {
      for (note in whiteKeys) {
        val noteWithOctave = NoteWithOctave(note, octave)

        // Stop if we've passed the actual end note
        if (noteWithOctave.absolutePosition > actualEnd.absolutePosition) {
          break
        }

        val x = whiteKeyIndex * whiteKeyWidth
        val isPressed =
          pressedNotes.any {
            it.note == note && it.octave == octave
          }

        drawWhiteKey(
          x = x,
          y = 0f,
          width = whiteKeyWidth,
          height = whiteKeyHeight,
          color = if (isPressed) pressedColor else whiteKeyColor,
          borderColor = borderColor,
        )

        whiteKeyIndex++
      }
    }

    // Draw black keys on top
    val blackKeyPositions =
      listOf(
        0.7f to Note.C_SHARP, // Between C and D
        1.7f to Note.D_SHARP, // Between D and E
        3.7f to Note.F_SHARP, // Between F and G
        4.7f to Note.G_SHARP, // Between G and A
        5.7f to Note.A_SHARP, // Between A and B
      )

    whiteKeyIndex = 0
    for (octave in startOctave..actualEnd.octave) {
      for ((position, note) in blackKeyPositions) {
        val noteWithOctave = NoteWithOctave(note, octave)

        // Stop if we've passed the actual end note
        if (noteWithOctave.absolutePosition > actualEnd.absolutePosition) {
          break
        }

        val x = (whiteKeyIndex + position) * whiteKeyWidth - blackKeyWidth / 2
        val isPressed =
          pressedNotes.any {
            it.note == note && it.octave == octave
          }

        drawBlackKey(
          x = x,
          y = 0f,
          width = blackKeyWidth,
          height = blackKeyHeight,
          color = if (isPressed) pressedColor else blackKeyColor,
          borderColor = borderColor,
        )
      }

      // Move white key index for next octave
      whiteKeyIndex += 7
    }
  }
}

/**
 * Calculate how many white keys to display from start octave to the end note
 */
private fun calculateWhiteKeysCount(
  startOctave: Int,
  endOctave: Int,
  endNote: NoteWithOctave,
): Int {
  val whiteNotes = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)

  var count = 0
  for (octave in startOctave..endOctave) {
    for (note in whiteNotes) {
      val currentNote = NoteWithOctave(note, octave)
      if (currentNote.absolutePosition <= endNote.absolutePosition) {
        count++
      } else {
        return count
      }
    }
  }

  return count
}

private fun DrawScope.drawWhiteKey(
  x: Float,
  y: Float,
  width: Float,
  height: Float,
  color: Color,
  borderColor: Color,
) {
  // Fill
  drawRect(
    color = color,
    topLeft = Offset(x, y),
    size = Size(width, height),
  )

  // Border
  drawRect(
    color = borderColor,
    topLeft = Offset(x, y),
    size = Size(width, height),
    style =
      androidx.compose.ui.graphics.drawscope
        .Stroke(width = 2f),
  )
}

private fun DrawScope.drawBlackKey(
  x: Float,
  y: Float,
  width: Float,
  height: Float,
  color: Color,
  borderColor: Color,
) {
  // Fill with rounded corners
  drawRoundRect(
    color = color,
    topLeft = Offset(x, y),
    size = Size(width, height),
    cornerRadius = CornerRadius(4f, 4f),
  )

  // Border
  drawRoundRect(
    color = borderColor,
    topLeft = Offset(x, y),
    size = Size(width, height),
    cornerRadius = CornerRadius(4f, 4f),
    style =
      androidx.compose.ui.graphics.drawscope
        .Stroke(width = 1.5f),
  )
}