package com.vireal.chordwizard.feature.pianorollui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

data class PianoKeyboardColors(
  val whiteKey: Color,
  val blackKey: Color,
  val border: Color,
  val pressedKey: Color,
  val targetDot: Color,
  val correctGlow: Color,
  val wrongGlow: Color,
  val wrongMark: Color,
)

@Composable
fun pianoKeyboardColors(
  whiteKey: Color = Color.White,
  blackKey: Color = Color.Black,
  border: Color = MaterialTheme.colorScheme.outline,
  pressedKey: Color = MaterialTheme.colorScheme.primary,
  targetDot: Color = Color(0xFF66FF9A),
  correctGlow: Color = MaterialTheme.colorScheme.tertiary,
  wrongGlow: Color = MaterialTheme.colorScheme.error,
  wrongMark: Color = Color.White,
): PianoKeyboardColors =
  PianoKeyboardColors(
    whiteKey = whiteKey,
    blackKey = blackKey,
    border = border,
    pressedKey = pressedKey,
    targetDot = targetDot,
    correctGlow = correctGlow,
    wrongGlow = wrongGlow,
    wrongMark = wrongMark,
  )

@Composable
fun PianoKeyboardView(
  pressedKeys: List<PressedKeyUi>,
  trainingSpec: PianoTrainingSpec? = null,
  trainingProgress: PianoTrainingProgress? = null,
  modifier: Modifier = Modifier,
  visibleRange: IntRange = 36..96,
  showTargetDots: Boolean = true,
  colors: PianoKeyboardColors = pianoKeyboardColors(),
) {
  val noteStateByMidi = buildNoteStateByMidi(visibleRange, pressedKeys, trainingSpec, trainingProgress)
  val targetNotes =
    if (showTargetDots) {
      trainingSpec?.targetSequence?.toSet().orEmpty()
    } else {
      emptySet()
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
        showTargetDot = note in targetNotes,
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
        showTargetDot = note in targetNotes,
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
  showTargetDot: Boolean,
  state: PianoKeyVisualState,
  colors: PianoKeyboardColors,
) {
  val fillColor =
    when (state) {
      PianoKeyVisualState.Idle -> colors.whiteKey
      PianoKeyVisualState.Pressed -> lerp(colors.whiteKey, colors.pressedKey, 0.62f)
      PianoKeyVisualState.TargetDot -> colors.whiteKey
      PianoKeyVisualState.CorrectPressed -> lerp(colors.whiteKey, colors.correctGlow, 0.42f)
      PianoKeyVisualState.WrongPressed -> lerp(colors.whiteKey, colors.wrongGlow, 0.36f)
    }

  drawRect(
    color = fillColor,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
  )

  when (state) {
    PianoKeyVisualState.CorrectPressed ->
      drawRect(
        brush =
          Brush.verticalGradient(
            colors = listOf(colors.correctGlow.copy(alpha = 0.45f), Color.Transparent),
            startY = height,
            endY = height * 0.42f,
          ),
        topLeft = Offset(x + 1f, 1f),
        size = Size(width - 2f, height - 2f),
        blendMode = BlendMode.SrcOver,
      )
    PianoKeyVisualState.WrongPressed ->
      drawRect(
        color = colors.wrongGlow.copy(alpha = 0.35f),
        topLeft = Offset(x + 2f, 2f),
        size = Size(width - 4f, height - 4f),
      )
    else -> Unit
  }

  if (showTargetDot) {
    drawCircle(
      color = colors.targetDot,
      radius = width * 0.10f,
      center = Offset(x + width * 0.50f, height * 0.83f),
    )
  }

  if (state == PianoKeyVisualState.WrongPressed) {
    drawWrongMark(
      center = Offset(x + width * 0.50f, height * 0.54f),
      size = width * 0.15f,
      color = colors.wrongMark,
      stroke = 3f,
    )
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
  showTargetDot: Boolean,
  state: PianoKeyVisualState,
  colors: PianoKeyboardColors,
) {
  val fillColor =
    when (state) {
      PianoKeyVisualState.Idle -> colors.blackKey
      PianoKeyVisualState.Pressed -> lerp(colors.blackKey, colors.pressedKey, 0.72f)
      PianoKeyVisualState.TargetDot -> colors.blackKey
      PianoKeyVisualState.CorrectPressed -> lerp(colors.blackKey, colors.correctGlow, 0.54f)
      PianoKeyVisualState.WrongPressed -> lerp(colors.blackKey, colors.wrongGlow, 0.66f)
    }

  drawRoundRect(
    color = fillColor,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
    cornerRadius = CornerRadius(4f, 4f),
  )

  when (state) {
    PianoKeyVisualState.CorrectPressed ->
      drawRoundRect(
        color = colors.correctGlow.copy(alpha = 0.82f),
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

  if (showTargetDot) {
    drawCircle(
      color = colors.targetDot,
      radius = width * 0.11f,
      center = Offset(x + width * 0.50f, height * 0.84f),
    )
  }

  if (state == PianoKeyVisualState.WrongPressed) {
    drawWrongMark(
      center = Offset(x + width * 0.50f, height * 0.47f),
      size = width * 0.18f,
      color = colors.wrongMark,
      stroke = 2.4f,
    )
  }

  drawRoundRect(
    color = colors.border,
    topLeft = Offset(x, 0f),
    size = Size(width, height),
    cornerRadius = CornerRadius(4f, 4f),
    style = Stroke(width = 1f),
  )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWrongMark(
  center: Offset,
  size: Float,
  color: Color,
  stroke: Float,
) {
  val half = size / 2f
  drawLine(
    color = color,
    start = Offset(center.x - half, center.y - half),
    end = Offset(center.x + half, center.y + half),
    strokeWidth = stroke,
  )
  drawLine(
    color = color,
    start = Offset(center.x - half, center.y + half),
    end = Offset(center.x + half, center.y - half),
    strokeWidth = stroke,
  )
}
