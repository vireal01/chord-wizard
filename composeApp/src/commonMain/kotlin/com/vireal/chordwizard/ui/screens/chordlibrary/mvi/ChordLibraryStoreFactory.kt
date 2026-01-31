package com.vireal.chordwizard.ui.screens.chordlibrary.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore.Intent
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore.Label
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore.State

internal class ChordLibraryStoreFactory(
  private val storeFactory: StoreFactory,
) {
  fun create(): ChordLibraryStore =
    object :
      ChordLibraryStore,
      Store<Intent, State, Label> by storeFactory.create(
        name = "ChordLibraryStore",
        initialState = State(),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl,
      ) {}

  private sealed interface Msg

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
    override fun executeIntent(intent: Intent) {
      when (intent) {
        is Intent.SelectChordRoot -> publish(Label.NavigateToChordDetails(intent.chordRoot))
        Intent.NavigateBack -> publish(Label.NavigateBack)
      }
    }
  }

  private object ReducerImpl : Reducer<State, Msg> {
    override fun State.reduce(msg: Msg): State = this
  }
}