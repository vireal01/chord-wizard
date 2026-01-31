package com.vireal.chordwizard.ui.screens.chorddetails.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.domain.model.ChordType
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore.Intent
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore.Label
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore.State
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ChordDetailsStoreFactory(
  private val storeFactory: StoreFactory,
  private val chordRoot: ChordRoot,
) {
  fun create(): ChordDetailsStore =
    object :
      ChordDetailsStore,
      Store<Intent, State, Label> by storeFactory.create(
        name = "ChordDetailsStore",
        initialState =
          State(
            chordRoot = chordRoot,
          ),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl,
      ) {}

  private sealed interface Msg {
    data object ToggleFavorite : Msg

    data object PlayStarted : Msg

    data object PlayStopped : Msg

    data class ChordTypeSelected(
      val chordType: ChordType,
    ) : Msg
  }

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
    override fun executeIntent(intent: Intent) {
      when (intent) {
        Intent.NavigateBack -> {
          publish(Label.NavigateBack)
        }

        Intent.ToggleFavorite -> {
          dispatch(Msg.ToggleFavorite)
          val message =
            if (!state().isFavorite) {
              "Added ${state().chordDisplayName} to favorites"
            } else {
              "Removed ${state().chordDisplayName} from favorites"
            }
          publish(Label.ShowToast(message))
        }

        Intent.PlayChord -> {
          dispatch(Msg.PlayStarted)
          scope.launch {
            delay(2000)
            dispatch(Msg.PlayStopped)
          }
          publish(Label.ShowToast("Playing ${state().chordDisplayName}"))
        }

        is Intent.SelectChordType -> {
          dispatch(Msg.ChordTypeSelected(intent.chordType))
          publish(Label.ShowToast("Selected ${state().chordDisplayName}"))
        }
      }
    }
  }

  private object ReducerImpl : Reducer<State, Msg> {
    override fun State.reduce(msg: Msg): State =
      when (msg) {
        Msg.ToggleFavorite -> copy(isFavorite = !isFavorite)
        Msg.PlayStarted -> copy(isPlaying = true)
        Msg.PlayStopped -> copy(isPlaying = false)
        is Msg.ChordTypeSelected -> copy(selectedChordType = msg.chordType)
      }
  }
}