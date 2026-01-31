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

/**
 * Piano keyboard component that highlights pressed notes
 */
@Composable
fun PianoKeyboard(
    pressedNotes: List<Note>,
    modifier: Modifier = Modifier,
    startOctave: Int = 4,
    octaveCount: Int = 1
) {
    val whiteKeyColor = MaterialTheme.colorScheme.surface
    val blackKeyColor = MaterialTheme.colorScheme.onSurface
    val pressedColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val whiteKeyWidth = size.width / (7 * octaveCount)
        val whiteKeyHeight = size.height
        val blackKeyWidth = whiteKeyWidth * 0.6f
        val blackKeyHeight = whiteKeyHeight * 0.6f

        // Draw white keys first
        val whiteKeys = listOf(
            Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B
        )

        for (octave in 0 until octaveCount) {
            whiteKeys.forEachIndexed { index, note ->
                val x = (octave * 7 + index) * whiteKeyWidth
                val isPressed = pressedNotes.contains(note)

                drawWhiteKey(
                    x = x,
                    y = 0f,
                    width = whiteKeyWidth,
                    height = whiteKeyHeight,
                    color = if (isPressed) pressedColor else whiteKeyColor,
                    borderColor = borderColor
                )
            }
        }

        // Draw black keys on top
        val blackKeyPositions = listOf(
            0.7f to Note.C_SHARP,  // Between C and D
            1.7f to Note.D_SHARP,  // Between D and E
            3.7f to Note.F_SHARP,  // Between F and G
            4.7f to Note.G_SHARP,  // Between G and A
            5.7f to Note.A_SHARP   // Between A and B
        )

        for (octave in 0 until octaveCount) {
            blackKeyPositions.forEach { (position, note) ->
                val x = (octave * 7 + position) * whiteKeyWidth - blackKeyWidth / 2
                val isPressed = pressedNotes.contains(note)

                drawBlackKey(
                    x = x,
                    y = 0f,
                    width = blackKeyWidth,
                    height = blackKeyHeight,
                    color = if (isPressed) pressedColor else blackKeyColor,
                    borderColor = borderColor
                )
            }
        }
    }
}

private fun DrawScope.drawWhiteKey(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color,
    borderColor: Color
) {
    // Fill
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height)
    )

    // Border
    drawRect(
        color = borderColor,
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
}

private fun DrawScope.drawBlackKey(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color,
    borderColor: Color
) {
    // Fill with rounded corners
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Border
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(4f, 4f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
    )
}
