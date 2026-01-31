package com.vireal.chordwizard.ui.screens.settings.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Intent
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.State
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Label

/**
 * Settings screen store interface
 */
interface SettingsStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object NavigateBack : Intent
        data object ToggleDarkMode : Intent
        data object ToggleNotifications : Intent
        data object ResetSettings : Intent
    }

    data class State(
        val darkMode: Boolean = false,
        val notifications: Boolean = true,
        val version: String = "1.0.0",
        val buildInfo: String = "Built with Kotlin Multiplatform & Compose Multiplatform",
        val isLoading: Boolean = false
    )

    sealed interface Label {
        data object NavigateBack : Label
        data class ShowToast(val message: String) : Label
    }
}
