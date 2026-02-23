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

sealed interface PianoValidationMode {
  data object StrictSequence : PianoValidationMode

  data object ChordSet : PianoValidationMode
}

enum class PitchMatchMode {
  ExactMidi,
  PitchClass,
}

data class PianoTrainingSpec(
  val targetSequence: List<Int>,
  val validationMode: PianoValidationMode = PianoValidationMode.StrictSequence,
  val pitchMatchMode: PitchMatchMode = PitchMatchMode.ExactMidi,
)

data class PianoTrainingProgress(
  val nextExpectedIndex: Int,
  val completedStepIndices: Set<Int>,
  val wrongActiveNotes: Set<Int>,
  val isCompleted: Boolean,
) {
  companion object {
    val Empty =
      PianoTrainingProgress(
        nextExpectedIndex = 0,
        completedStepIndices = emptySet(),
        wrongActiveNotes = emptySet(),
        isCompleted = false,
      )
  }
}

enum class PianoKeyVisualState {
  Idle,
  Pressed,
  TargetDot,
  CorrectPressed,
  WrongPressed,
}
