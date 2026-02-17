package com.vireal.chordwizard.feature.pianorollui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

data class PianoKeyboardColors(
  val whiteKey: Color,
  val blackKey: Color,
  val border: Color,
  val pressedKey: Color,
  val targetGlow: Color,
  val wrongGlow: Color,
)

@Composable
fun pianoKeyboardColors(
  whiteKey: Color = Color.White,
  blackKey: Color = Color.Black,
  border: Color = MaterialTheme.colorScheme.outline,
  pressedKey: Color = MaterialTheme.colorScheme.primary,
  targetGlow: Color = MaterialTheme.colorScheme.tertiary,
  wrongGlow: Color = MaterialTheme.colorScheme.error,
): PianoKeyboardColors =
  PianoKeyboardColors(
    whiteKey = whiteKey,
    blackKey = blackKey,
    border = border,
    pressedKey = pressedKey,
    targetGlow = targetGlow,
    wrongGlow = wrongGlow,
  )

@Composable
fun PianoKeyboardView(
  pressedKeys: List<PressedKeyUi>,
  targetNotes: Set<Int>,
  modifier: Modifier = Modifier,
  visibleRange: IntRange = 36..96,
  colors: PianoKeyboardColors = pianoKeyboardColors(),
) {
  val noteStateByMidi =
    buildMap<Int, PianoKeyVisualState> {
      val pressedByNote = pressedKeys.associateBy { it.note }
      for (note in visibleRange) {
        val isTarget = note in targetNotes
        val isPressed = pressedByNote.containsKey(note)
        val isWrongPressed = isPressed && targetNotes.isNotEmpty() && !isTarget

        val state =
          when {
            isWrongPressed -> PianoKeyVisualState.WrongPressed
            isPressed -> PianoKeyVisualState.Pressed
            isTarget -> PianoKeyVisualState.Target
            else -> PianoKeyVisualState.Idle
          }

        put(note, state)
      }
    }

  val whiteNotes = visibleRange.filter(::isWhiteKey)
  if (whiteNotes.isEmpty()) return

  val whiteIndexByNote = whiteNotes.withIndex().associate { (index, note) -> note to index }

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(220.dp),
  ) {
    val whiteKeyWidth = size.width / whiteNotes.size.toFloat()
    val whiteKeyHeight = size.height
    val blackKeyWidth = whiteKeyWidth * 0.62f
    val blackKeyHeight = whiteKeyHeight * 0.63f

    for (note in whiteNotes) {
      val index = whiteIndexByNote.getValue(note)
      val x = index * whiteKeyWidth
      val state = noteStateByMidi[note] ?: PianoKeyVisualState.Idle
      drawWhiteKey(
        x = x,
        width = whiteKeyWidth,
        height = whiteKeyHeight,
        state = state,
        colors = colors,
      )
    }

    for (note in visibleRange.filter(::isBlackKey)) {
      val previousWhite = note - 1
      val whiteIndex = whiteIndexByNote[previousWhite] ?: continue
      val x = (whiteIndex + 1) * whiteKeyWidth - (blackKeyWidth / 2f)
      val state = noteStateByMidi[note] ?: PianoKeyVisualState.Idle
      drawBlackKey(
        x = x,
        width = blackKeyWidth,
        height = blackKeyHeight,
        state = state,
        colors = colors,
      )
    }
  }
}

private fun isWhiteKey(note: Int): Boolean = note % 12 in setOf(0, 2, 4, 5, 7, 9, 11)

private fun isBlackKey(note: Int): Boolean = note % 12 in setOf(1, 3, 6, 8, 10)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWhiteKey(
  x: Float,
  width: Float,
  height: Float,
  state: PianoKeyVisualState,
  colors: PianoKeyboardColors,
) {
  val fillColor =
    when (state) {
      PianoKeyVisualState.Idle -> colors.whiteKey
      PianoKeyVisualState.Pressed -> lerp(colors.whiteKey, colors.pressedKey, 0.62f)
      PianoKeyVisualState.Target -> colors.whiteKey
      PianoKeyVisualState.WrongPressed -> lerp(colors.whiteKey, colors.wrongGlow, 0.36f)
    }

  drawRect(
    color = fillColor,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
  )

  when (state) {
    PianoKeyVisualState.Target ->
      drawRect(
        color = colors.targetGlow.copy(alpha = 0.28f),
        topLeft = Offset(x + 2f, 2f),
        size = Size(width - 4f, height - 4f),
      )
    PianoKeyVisualState.WrongPressed ->
      drawRect(
        color = colors.wrongGlow.copy(alpha = 0.35f),
        topLeft = Offset(x + 2f, 2f),
        size = Size(width - 4f, height - 4f),
      )
    else -> Unit
  }

  drawRect(
    color = colors.border,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
    style = Stroke(width = 1.2f),
  )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlackKey(
  x: Float,
  width: Float,
  height: Float,
  state: PianoKeyVisualState,
  colors: PianoKeyboardColors,
) {
  val fillColor =
    when (state) {
      PianoKeyVisualState.Idle -> colors.blackKey
      PianoKeyVisualState.Pressed -> lerp(colors.blackKey, colors.pressedKey, 0.72f)
      PianoKeyVisualState.Target -> colors.blackKey
      PianoKeyVisualState.WrongPressed -> lerp(colors.blackKey, colors.wrongGlow, 0.66f)
    }

  drawRoundRect(
    color = fillColor,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
    cornerRadius = CornerRadius(4f, 4f),
  )

  when (state) {
    PianoKeyVisualState.Target ->
      drawRoundRect(
        color = colors.targetGlow.copy(alpha = 0.75f),
        topLeft = Offset(x - 1.5f, -1.5f),
        size = Size(width + 3f, height + 3f),
        cornerRadius = CornerRadius(5f, 5f),
        style = Stroke(width = 3f),
      )
    PianoKeyVisualState.WrongPressed ->
      drawRoundRect(
        color = colors.wrongGlow.copy(alpha = 0.82f),
        topLeft = Offset(x - 1.5f, -1.5f),
        size = Size(width + 3f, height + 3f),
        cornerRadius = CornerRadius(5f, 5f),
        style = Stroke(width = 3f),
      )
    else -> Unit
  }

  drawRoundRect(
    color = colors.border,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
    cornerRadius = CornerRadius(4f, 4f),
    style = Stroke(width = 1f),
  )
}
