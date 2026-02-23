package com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.feature.pianorollui.PianoTrainingProgress
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerAdvanceMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerCard
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.model.normalizePitchClass
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore.Intent
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore.Label
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore.State

interface ChordTrainerSessionStore : Store<Intent, State, Label> {
  sealed interface Intent {
    data class UpdateMidiNotes(
      val notes: Set<Int>,
    ) : Intent

    data class ToggleTouchPitchClass(
      val pitchClass: Int,
    ) : Intent

    data object ToggleHints : Intent

    data object ToggleNotation : Intent

    data object RevealAnswer : Intent

    data object NextChord : Intent

    data object SkipChord : Intent

    data object NavigateBack : Intent
  }

  data class State(
    val config: TrainerSessionConfig,
    val deck: List<TrainerCard>,
    val currentIndex: Int = 0,
    val midiNotes: Set<Int> = emptySet(),
    val touchNotes: Set<Int> = emptySet(),
    val hintsEnabled: Boolean = config.mode == TrainerMode.Guided,
    val notationEnabled: Boolean = true,
    val answerRevealed: Boolean = config.mode == TrainerMode.Guided,
    val solvedCount: Int = 0,
    val skippedCount: Int = 0,
    val mistakesCount: Int = 0,
    val currentCardHadMistake: Boolean = false,
    val currentCardSolved: Boolean = false,
  ) {
    val isFinished: Boolean
      get() = deck.isEmpty() || currentIndex >= deck.size

    val currentCard: TrainerCard?
      get() = deck.getOrNull(currentIndex)

    val activeNotes: Set<Int>
      get() = midiNotes + touchNotes

    val targetSequence: List<Int>
      get() = currentCard?.toTargetMidiSequence().orEmpty()

    val targetPitchClasses: Set<Int>
      get() = currentCard?.targetPitchClasses.orEmpty()

    val activePitchClasses: Set<Int>
      get() = activeNotes.map(::normalizePitchClass).toSet()

    val missingPitchClasses: Set<Int>
      get() = targetPitchClasses - activePitchClasses

    val wrongActiveNotes: Set<Int>
      get() = activeNotes.filterTo(linkedSetOf()) { normalizePitchClass(it) !in targetPitchClasses }

    val completedStepIndices: Set<Int>
      get() =
        targetSequence
          .mapIndexedNotNull { index, midi ->
            index.takeIf {
              normalizePitchClass(midi) in
                activePitchClasses
            }
          }.toSet()

    val isCurrentCorrect: Boolean
      get() =
        !isFinished &&
          targetPitchClasses.isNotEmpty() &&
          missingPitchClasses.isEmpty() &&
          wrongActiveNotes.isEmpty()

    val isCurrentSolved: Boolean
      get() = currentCardSolved || isCurrentCorrect

    val answerVisible: Boolean
      get() =
        isFinished ||
          config.mode == TrainerMode.Guided ||
          hintsEnabled ||
          answerRevealed ||
          isCurrentSolved

    val canAdvanceManually: Boolean
      get() =
        !isFinished &&
          config.advanceMode == TrainerAdvanceMode.Manual &&
          isCurrentSolved

    val shouldAutoAdvance: Boolean
      get() =
        !isFinished &&
          config.advanceMode == TrainerAdvanceMode.Auto &&
          isCurrentSolved

    val progressCurrent: Int
      get() = if (deck.isEmpty()) 0 else (currentIndex + 1).coerceAtMost(deck.size)

    val progressTotal: Int
      get() = deck.size

    val accuracyPercent: Int
      get() {
        val attempts = solvedCount + skippedCount
        if (attempts == 0) return 0
        val successRate = (solvedCount.toFloat() / attempts.toFloat()) * 100f
        return successRate.toInt().coerceIn(0, 100)
      }

    val trainingProgress: PianoTrainingProgress
      get() =
        PianoTrainingProgress(
          nextExpectedIndex = completedStepIndices.size,
          completedStepIndices = completedStepIndices,
          wrongActiveNotes = wrongActiveNotes,
          isCompleted = isCurrentSolved,
        )
  }

  sealed interface Label {
    data object NavigateBack : Label
  }
}