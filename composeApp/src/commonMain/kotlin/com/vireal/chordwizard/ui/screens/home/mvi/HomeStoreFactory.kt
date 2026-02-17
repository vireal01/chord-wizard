package com.vireal.chordwizard.ui.screens.home.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.di.AppRepository
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.Intent
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.Label
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.State
import kotlinx.coroutines.launch

/**
 * Factory to create HomeStore
 */
internal class HomeStoreFactory(
  private val storeFactory: StoreFactory,
  private val repository: AppRepository,
  private val midiInputService: MidiInputService,
  private val observeActiveMidiNotesUseCase: ObserveActiveMidiNotesUseCase,
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
        bootstrapper = SimpleBootstrapper(Action.InitializeMidi, Action.ObserveMidiNotes),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl,
      ) {}

  private sealed interface Action {
    data object InitializeMidi : Action

    data object ObserveMidiNotes : Action
  }

  /**
   * Executor - handles intents and produces messages/labels
   */
  private sealed interface Msg {
    data object ToggleContent : Msg

    data class ActiveMidiNotesChanged(
      val notes: List<ActiveMidiNote>,
    ) : Msg
  }

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Action, State, Msg, Label>() {
    override fun executeAction(action: Action) {
      when (action) {
        Action.InitializeMidi -> {
          scope.launch {
            midiInputService.refreshAvailability()
            midiInputService.startScan()
          }

          scope.launch {
            midiInputService.discoveredDevices.collect { devices ->
              if (devices.isEmpty()) {
                return@collect
              }

              when (midiInputService.connectionState.value) {
                MidiConnectionState.Disconnected -> midiInputService.connect(devices.first().id)
                is MidiConnectionState.Failed -> midiInputService.connect(devices.first().id)
                else -> Unit
              }
            }
          }
        }

        Action.ObserveMidiNotes -> {
          scope.launch {
            observeActiveMidiNotesUseCase.execute().collect { notes ->
              dispatch(Msg.ActiveMidiNotesChanged(notes))
            }
          }
        }
      }
    }

    override fun executeIntent(intent: Intent) {
      when (intent) {
        Intent.ToggleContent -> dispatch(Msg.ToggleContent)
        Intent.NavigateToChordLibrary -> publish(Label.NavigateToChordLibrary)
        Intent.NavigateToSettings -> publish(Label.NavigateToSettings)
        Intent.NavigateToNoteVisualizer -> publish(Label.NavigateToNoteVisualizer)
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
        is Msg.ActiveMidiNotesChanged -> copy(activeMidiNotes = msg.notes)
      }
  }
}