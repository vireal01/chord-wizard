package com.vireal.chordwizard.feature.pianorollui

data class PressedKeyUi(
  val note: Int,
  val velocity: Int,
  val isTarget: Boolean,
  val isCorrect: Boolean,
  val startedAt: Long,
)

data class RollNoteUi(
  val note: Int,
  val channel: Int,
  val startMs: Long,
  val endMs: Long?,
  val velocity: Int,
  val sourceDeviceId: String,
  val kind: RollNoteKind = RollNoteKind.LIVE,
)

enum class RollNoteKind {
  LIVE,
  TARGET,
}

data class PianoViewportUi(
  val visibleRange: IntRange,
  val zoom: Float,
  val scrollOffset: Float,
)

enum class PianoKeyVisualState {
  Idle,
  Pressed,
  Target,
  WrongPressed,
}
