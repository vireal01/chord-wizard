package com.vireal.chordwizard.ui.screens.home.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.Intent
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.Label
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore.State

/**
 * Home screen store interface
 * Defines the contract for MVI pattern
 */
interface HomeStore : Store<Intent, State, Label> {
  /**
   * User intents - actions that user can perform
   */
  sealed interface Intent {
    data object ToggleContent : Intent

    data object NavigateToChordLibrary : Intent

    data object NavigateToSettings : Intent
  }

  /**
   * UI State - represents the current state of the screen
   */
  data class State(
    val greeting: String = "",
    val appInfo: String = "",
    val showContent: Boolean = false,
    val isLoading: Boolean = false,
    val activeMidiNotes: List<ActiveMidiNote> = emptyList(),
  )

  /**
   * Side effects (one-time events) - navigation, toasts, etc.
   */
  sealed interface Label {
    data object NavigateToChordLibrary : Label

    data object NavigateToSettings : Label
  }
}
