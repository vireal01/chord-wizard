package com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerAdvanceMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerChordFamily
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerRoot
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerMode
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore.Intent
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore.Label
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore.State

internal class ChordTrainerSetupStoreFactory(
  private val storeFactory: StoreFactory,
  private val trainerSetupMemoryRepository: TrainerSetupMemoryRepository,
) {
  fun create(): ChordTrainerSetupStore =
    object :
      ChordTrainerSetupStore,
      Store<Intent, State, Label> by storeFactory.create(
        name = "ChordTrainerSetupStore",
        initialState = trainerSetupMemoryRepository.get()?.toState() ?: State(),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl(),
      ) {}

  private sealed interface Msg {
    data class FamilyToggled(
      val family: TrainerChordFamily,
    ) : Msg

    data class RootToggled(
      val root: TrainerRoot,
    ) : Msg

    data class ModeSelected(
      val mode: TrainerMode,
    ) : Msg

    data class AdvanceModeSelected(
      val mode: TrainerAdvanceMode,
    ) : Msg

    data class CardCountUpdated(
      val count: Int,
    ) : Msg
  }

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
    override fun executeIntent(intent: Intent) {
      when (intent) {
        is Intent.ToggleFamily -> dispatch(Msg.FamilyToggled(intent.family))
        is Intent.ToggleRoot -> dispatch(Msg.RootToggled(intent.root))
        is Intent.SelectMode -> dispatch(Msg.ModeSelected(intent.mode))
        is Intent.SelectAdvanceMode -> dispatch(Msg.AdvanceModeSelected(intent.mode))
        is Intent.SetCardCount -> dispatch(Msg.CardCountUpdated(intent.count))
        Intent.NavigateBack -> publish(Label.NavigateBack)
        Intent.StartTraining -> {
          val currentState = state()
          if (currentState.startEnabled) {
            publish(Label.NavigateToSession(currentState.toSessionConfig()))
          }
        }
      }
    }
  }

  private inner class ReducerImpl : Reducer<State, Msg> {
    override fun State.reduce(msg: Msg): State =
      when (msg) {
        is Msg.FamilyToggled ->
          copy(
            selectedFamilies =
              if (msg.family in selectedFamilies) {
                selectedFamilies - msg.family
              } else {
                selectedFamilies + msg.family
              },
          )

        is Msg.RootToggled ->
          copy(
            selectedRoots =
              if (msg.root in selectedRoots) {
                selectedRoots - msg.root
              } else {
                selectedRoots + msg.root
              },
          )

        is Msg.ModeSelected -> copy(selectedMode = msg.mode)
        is Msg.AdvanceModeSelected -> copy(selectedAdvanceMode = msg.mode)
        is Msg.CardCountUpdated -> copy(cardCount = msg.count.coerceAtLeast(1))
      }.also { nextState ->
        trainerSetupMemoryRepository.save(nextState.toSessionConfig())
      }
  }
}

private fun TrainerSessionConfig.toState(): State =
  State(
    selectedFamilies = selectedFamilies,
    selectedRoots = selectedRoots,
    selectedMode = mode,
    selectedAdvanceMode = advanceMode,
    cardCount = cardCount,
  )
