package com.vireal.chordwizard.ui.screens.settings.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Intent
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Label
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.State

internal class SettingsStoreFactory(
    private val storeFactory: StoreFactory
) {

    fun create(): SettingsStore =
        object : SettingsStore, Store<Intent, State, Label> by storeFactory.create(
            name = "SettingsStore",
            initialState = State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed interface Msg {
        data object ToggleDarkMode : Msg
        data object ToggleNotifications : Msg
        data object ResetSettings : Msg
    }

    private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {
        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.NavigateBack -> publish(Label.NavigateBack)
                Intent.ToggleDarkMode -> {
                    dispatch(Msg.ToggleDarkMode)
                    val message = if (state().darkMode) {
                        "Dark mode enabled"
                    } else {
                        "Dark mode disabled"
                    }
                    publish(Label.ShowToast(message))
                }
                Intent.ToggleNotifications -> {
                    dispatch(Msg.ToggleNotifications)
                    val message = if (state().notifications) {
                        "Notifications enabled"
                    } else {
                        "Notifications disabled"
                    }
                    publish(Label.ShowToast(message))
                }
                Intent.ResetSettings -> {
                    dispatch(Msg.ResetSettings)
                    publish(Label.ShowToast("Settings reset to defaults"))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State =
            when (msg) {
                Msg.ToggleDarkMode -> copy(darkMode = !darkMode)
                Msg.ToggleNotifications -> copy(notifications = !notifications)
                Msg.ResetSettings -> State() // Reset to defaults
            }
    }
}
