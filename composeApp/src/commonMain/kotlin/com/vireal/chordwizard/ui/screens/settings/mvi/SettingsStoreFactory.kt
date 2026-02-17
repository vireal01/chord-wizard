package com.vireal.chordwizard.ui.screens.settings.mvi

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiDevice
import com.vireal.chordwizard.midi.core.MidiError
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.core.MidiScanState
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Intent
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Label
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.State
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class SettingsStoreFactory(
  private val storeFactory: StoreFactory,
  private val midiInputService: MidiInputService,
) {
  fun create(): SettingsStore =
    object :
      SettingsStore,
      Store<Intent, State, Label> by storeFactory.create(
        name = "SettingsStore",
        initialState = State(),
        bootstrapper = SimpleBootstrapper(Action.ObserveMidiState),
        executorFactory = ::ExecutorImpl,
        reducer = ReducerImpl,
      ) {}

  private sealed interface Action {
    data object ObserveMidiState : Action
  }

  private sealed interface Msg {
    data object ToggleDarkMode : Msg

    data object ToggleNotifications : Msg

    data object ResetSettings : Msg

    data class MidiAvailabilityChanged(
      val availability: MidiAvailability,
    ) : Msg

    data class MidiScanStateChanged(
      val scanState: MidiScanState,
    ) : Msg

    data class MidiConnectionStateChanged(
      val connectionState: MidiConnectionState,
    ) : Msg

    data class MidiDevicesChanged(
      val devices: List<MidiDevice>,
    ) : Msg

    data class MidiErrorChanged(
      val message: String,
    ) : Msg
  }

  private inner class ExecutorImpl : CoroutineExecutor<Intent, Action, State, Msg, Label>() {
    override fun executeAction(action: Action) {
      when (action) {
        Action.ObserveMidiState -> {
          scope.launch {
            midiInputService.refreshAvailability()
          }

          scope.launch {
            midiInputService.availability.collect { availability ->
              dispatch(Msg.MidiAvailabilityChanged(availability))
            }
          }

          scope.launch {
            midiInputService.scanState.collect { scanState ->
              dispatch(Msg.MidiScanStateChanged(scanState))
            }
          }

          scope.launch {
            midiInputService.discoveredDevices.collect { devices ->
              dispatch(Msg.MidiDevicesChanged(devices))
            }
          }

          scope.launch {
            midiInputService.connectionState.collect { connectionState ->
              dispatch(Msg.MidiConnectionStateChanged(connectionState))
            }
          }

          scope.launch {
            midiInputService.errors.collect { error ->
              val message = error.toUiMessage()
              dispatch(Msg.MidiErrorChanged(message))
              publish(Label.ShowToast(message))
            }
          }
        }
      }
    }

    override fun executeIntent(intent: Intent) {
      when (intent) {
        Intent.NavigateBack -> {
          publish(Label.NavigateBack)
        }

        Intent.ToggleDarkMode -> {
          dispatch(Msg.ToggleDarkMode)
          val message =
            if (state().darkMode) {
              "Dark mode enabled"
            } else {
              "Dark mode disabled"
            }
          publish(Label.ShowToast(message))
        }

        Intent.ToggleNotifications -> {
          dispatch(Msg.ToggleNotifications)
          val message =
            if (state().notifications) {
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

        Intent.ScanForMidiDevices -> {
          scope.launch {
            midiInputService.startScan()
          }
        }

        Intent.StopMidiScan -> {
          scope.launch {
            midiInputService.stopScan()
          }
        }

        is Intent.ConnectMidiDevice -> {
          scope.launch {
            midiInputService.connect(intent.deviceId)
          }
        }

        Intent.DisconnectMidiDevice -> {
          scope.launch {
            midiInputService.disconnect()
          }
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
        is Msg.MidiAvailabilityChanged -> copy(midiAvailability = msg.availability)
        is Msg.MidiScanStateChanged -> copy(midiScanState = msg.scanState)
        is Msg.MidiConnectionStateChanged -> copy(midiConnectionState = msg.connectionState)
        is Msg.MidiDevicesChanged -> copy(midiDevices = msg.devices)
        is Msg.MidiErrorChanged -> copy(midiLastError = msg.message)
      }
  }
}

private fun MidiError.toUiMessage(): String =
  when (this) {
    is MidiError.ScanFailed -> message
    is MidiError.ConnectionFailed -> message
    is MidiError.ProtocolError -> message
    is MidiError.UnsupportedPlatform -> message
    is MidiError.Unknown -> message
  }
