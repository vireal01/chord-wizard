package com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerDeckGenerator
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.normalizePitchClass
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore.Intent
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore.Label
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore.State

internal class ChordTrainerSessionStoreFactory(
  private val storeFactory: StoreFactory,
  private val config: TrainerSessionConfig,
) {
  fun create(): ChordTrainerSessionStore =
    object :
      ChordTrainerSessionStore,
      Store<Intent, State, Label> by storeFactory.create(
        name = "ChordTrainerSessionStore",
        initialState =
          State(
            config = config,
            deck = TrainerDeckGenerator.buildDeck(config),
            hintsEnabled = config.mode == TrainerMode.Guided,
            notationEnabled = true,
            answerRevealed = config.mode == TrainerMode.Guided,
          ),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl,
      ) {}

  private sealed interface Msg {
    data class MidiNotesUpdated(
      val notes: Set<Int>,
    ) : Msg

    data class TouchPitchClassToggled(
      val pitchClass: Int,
    ) : Msg

    data object HintsToggled : Msg

    data object NotationToggled : Msg

    data object AnswerRevealed : Msg

    data object NextChord : Msg

    data object SkipChord : Msg
  }

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
    override fun executeIntent(intent: Intent) {
      when (intent) {
        is Intent.UpdateMidiNotes -> dispatch(Msg.MidiNotesUpdated(intent.notes))
        is Intent.ToggleTouchPitchClass -> dispatch(Msg.TouchPitchClassToggled(intent.pitchClass))
        Intent.ToggleHints -> dispatch(Msg.HintsToggled)
        Intent.ToggleNotation -> dispatch(Msg.NotationToggled)
        Intent.RevealAnswer -> dispatch(Msg.AnswerRevealed)
        Intent.NextChord -> {
          if (state().isCurrentSolved) {
            dispatch(Msg.NextChord)
          }
        }
        Intent.SkipChord -> dispatch(Msg.SkipChord)
        Intent.NavigateBack -> publish(Label.NavigateBack)
      }
    }
  }

  private object ReducerImpl : Reducer<State, Msg> {
    override fun State.reduce(msg: Msg): State =
      when (msg) {
        is Msg.MidiNotesUpdated -> withUpdatedActiveNotes(midi = msg.notes)
        is Msg.TouchPitchClassToggled -> withTouchPitchClassToggled(msg.pitchClass)
        Msg.HintsToggled -> copy(hintsEnabled = !hintsEnabled)
        Msg.NotationToggled -> copy(notationEnabled = !notationEnabled)
        Msg.AnswerRevealed -> copy(answerRevealed = true)
        Msg.NextChord -> advanceToNext(isSkipped = false)
        Msg.SkipChord -> advanceToNext(isSkipped = true)
      }

    private fun State.withUpdatedActiveNotes(
      midi: Set<Int> = midiNotes,
      touch: Set<Int> = touchNotes,
    ): State {
      val updated =
        copy(
          midiNotes = midi,
          touchNotes = touch,
        )
      val withSolvedLatch =
        if (updated.isCurrentCorrect) {
          updated.copy(currentCardSolved = true)
        } else {
          updated
        }
      return withSolvedLatch.markMistakeIfNeeded()
    }

    private fun State.withTouchPitchClassToggled(pitchClass: Int): State {
      val normalized = normalizePitchClass(pitchClass)
      val midiNote = 60 + normalized
      val updatedTouchNotes =
        if (midiNote in touchNotes) {
          touchNotes - midiNote
        } else {
          touchNotes + midiNote
        }

      return withUpdatedActiveNotes(touch = updatedTouchNotes)
    }

    private fun State.markMistakeIfNeeded(): State {
      val shouldMark = !isFinished && activeNotes.isNotEmpty() && !isCurrentCorrect
      if (!shouldMark || currentCardHadMistake) return this
      return copy(currentCardHadMistake = true)
    }

    private fun State.advanceToNext(isSkipped: Boolean): State {
      if (isFinished) return this
      val nextIndex = (currentIndex + 1).coerceAtMost(deck.size)
      val mistakeDelta = if (currentCardHadMistake) 1 else 0
      val guidedMode = config.mode == TrainerMode.Guided

      return copy(
        currentIndex = nextIndex,
        touchNotes = emptySet(),
        answerRevealed = guidedMode,
        currentCardHadMistake = false,
        currentCardSolved = false,
        solvedCount = solvedCount + if (isSkipped) 0 else 1,
        skippedCount = skippedCount + if (isSkipped) 1 else 0,
        mistakesCount = mistakesCount + mistakeDelta,
      )
    }
  }
}
