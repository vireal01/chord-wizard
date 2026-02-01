package com.vireal.chordwizard.ui.screens.home.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.di.AppRepository
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.Intent
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.Label
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.State

/**
 * Factory to create HomeStore
 */
internal class HomeStoreFactory(
  private val storeFactory: StoreFactory,
  private val repository: AppRepository,
) {
  fun create(): HomeStore =
    object :
      HomeStore,
      Store<Intent, State, Label> by storeFactory.create(
        name = "HomeStore",
        initialState =
          State(
            greeting = repository.getGreeting(),
            appInfo = repository.getAppInfo(),
          ),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl,
      ) {}

  /**
   * Executor - handles intents and produces messages/labels
   */
  private sealed interface Msg {
    data object ToggleContent : Msg
  }

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
    override fun executeIntent(intent: Intent) {
      when (intent) {
        Intent.ToggleContent -> dispatch(Msg.ToggleContent)
        Intent.NavigateToChordLibrary -> publish(Label.NavigateToChordLibrary)
        Intent.NavigateToSettings -> publish(Label.NavigateToSettings)
      }
    }
  }

  /**
   * Reducer - applies messages to state
   */
  private object ReducerImpl : Reducer<State, Msg> {
    override fun State.reduce(msg: Msg): State =
      when (msg) {
        Msg.ToggleContent -> copy(showContent = !showContent)
      }
  }
}