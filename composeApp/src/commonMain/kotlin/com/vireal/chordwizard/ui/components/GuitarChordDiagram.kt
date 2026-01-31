package com.vireal.chordwizard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vireal.chordwizard.domain.model.Chord

/**
 * Guitar fretboard diagram showing finger positions
 * Data class representing finger position on guitar
 */
data class GuitarPosition(
  val string: Int, // 1-6 (1 = high E, 6 = low E)
  val fret: Int, // 0 = open, X = muted
  val finger: Int, // 1-4, or 0 for open
)

/**
 * Simple chord positions database
 */
object ChordPositions {
  fun getPositions(chord: Chord): List<GuitarPosition> {
    // Simplified positions for demonstration
    // TODO: Expand with complete chord database
    return when (chord.root.displayName to chord.type.displayName) {
      "C" to "Major" -> {
        listOf(
          GuitarPosition(1, 0, 0), // High E - open
          GuitarPosition(2, 1, 1), // B - 1st fret
          GuitarPosition(3, 0, 0), // G - open
          GuitarPosition(4, 2, 2), // D - 2nd fret
          GuitarPosition(5, 3, 3), // A - 3rd fret
          GuitarPosition(6, -1, 0), // Low E - muted
        )
      }

      "G" to "Major" -> {
        listOf(
          GuitarPosition(1, 3, 3),
          GuitarPosition(2, 0, 0),
          GuitarPosition(3, 0, 0),
          GuitarPosition(4, 0, 0),
          GuitarPosition(5, 2, 2),
          GuitarPosition(6, 3, 4),
        )
      }

      "D" to "Major" -> {
        listOf(
          GuitarPosition(1, 2, 2),
          GuitarPosition(2, 3, 3),
          GuitarPosition(3, 2, 1),
          GuitarPosition(4, 0, 0),
          GuitarPosition(5, -1, 0),
          GuitarPosition(6, -1, 0),
        )
      }

      else -> {
        listOf()
      } // Default empty for now
    }
  }
}

/**
 * Guitar chord diagram component
 */
@Composable
fun GuitarChordDiagram(
  chord: Chord,
  modifier: Modifier = Modifier,
) {
  val positions = ChordPositions.getPositions(chord)

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = chord.displayName,
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    if (positions.isEmpty()) {
      Text(
        text = "Chord diagram coming soon",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      GuitarFretboard(positions = positions)
    }
  }
}

@Composable
private fun GuitarFretboard(
  positions: List<GuitarPosition>,
  modifier: Modifier = Modifier,
) {
  val stringColor = MaterialTheme.colorScheme.onSurface
  val fretColor = MaterialTheme.colorScheme.onSurfaceVariant
  val dotColor = MaterialTheme.colorScheme.primary

  Canvas(
    modifier =
      modifier
        .width(200.dp)
        .height(250.dp)
        .padding(16.dp),
  ) {
    val fretCount = 4
    val stringCount = 6
    val stringSpacing = size.width / (stringCount - 1)
    val fretSpacing = size.height / (fretCount + 1)
    val startY = fretSpacing * 0.5f

    // Draw strings (vertical lines)
    for (i in 0 until stringCount) {
      val x = i * stringSpacing
      drawLine(
        color = stringColor,
        start = Offset(x, startY),
        end = Offset(x, size.height),
        strokeWidth = if (i == 0 || i == stringCount - 1) 3f else 2f,
      )
    }

    // Draw frets (horizontal lines)
    for (i in 0..fretCount) {
      val y = startY + i * fretSpacing
      drawLine(
        color = fretColor,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = if (i == 0) 5f else 2f, // Nut is thicker
      )
    }

    // Draw finger positions
    positions.forEach { position ->
      val stringIndex = stringCount - position.string // Reverse for display
      val x = stringIndex * stringSpacing

      when {
        position.fret == -1 -> {
          // Draw X for muted string
          drawX(
            center = Offset(x, startY * 0.3f),
            size = 15f,
            color = stringColor,
          )
        }

        position.fret == 0 -> {
          // Draw O for open string
          drawCircle(
            color = stringColor,
            radius = 8f,
            center = Offset(x, startY * 0.3f),
            style = Stroke(width = 2f),
          )
        }

        else -> {
          // Draw filled circle for fretted note
          val y = startY + (position.fret - 0.5f) * fretSpacing
          drawCircle(
            color = dotColor,
            radius = 12f,
            center = Offset(x, y),
          )

          // Draw white dot inside for contrast
          if (position.finger > 0) {
            drawCircle(
              color = Color.White,
              radius = 5f,
              center = Offset(x, y),
            )
          }
        }
      }
    }
  }
}

private fun DrawScope.drawX(
  center: Offset,
  size: Float,
  color: Color,
) {
  val halfSize = size / 2
  drawLine(
    color = color,
    start = Offset(center.x - halfSize, center.y - halfSize),
    end = Offset(center.x + halfSize, center.y + halfSize),
    strokeWidth = 3f,
  )
  drawLine(
    color = color,
    start = Offset(center.x - halfSize, center.y + halfSize),
    end = Offset(center.x + halfSize, center.y - halfSize),
    strokeWidth = 3f,
  )
}