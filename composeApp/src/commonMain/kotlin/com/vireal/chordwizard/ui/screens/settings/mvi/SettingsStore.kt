package com.vireal.chordwizard.ui.screens.settings.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiDevice
import com.vireal.chordwizard.midi.core.MidiScanState
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Intent
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.Label
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore.State

/**
 * Settings screen store interface
 */
interface SettingsStore : Store<Intent, State, Label> {
  sealed interface Intent {
    data object NavigateBack : Intent

    data object ToggleDarkMode : Intent

    data object ToggleNotifications : Intent

    data object ResetSettings : Intent

    data object ScanForMidiDevices : Intent

    data object StopMidiScan : Intent

    data class ConnectMidiDevice(
      val deviceId: String,
    ) : Intent

    data object DisconnectMidiDevice : Intent
  }

  data class State(
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val version: String = "1.0.0",
    val buildInfo: String = "Built with Kotlin Multiplatform & Compose Multiplatform",
    val isLoading: Boolean = false,
    val midiAvailability: MidiAvailability = MidiAvailability(MidiAvailability.Status.UNAVAILABLE),
    val midiScanState: MidiScanState = MidiScanState.Idle,
    val midiConnectionState: MidiConnectionState = MidiConnectionState.Disconnected,
    val midiDevices: List<MidiDevice> = emptyList(),
    val midiLastError: String? = null,
  )

  sealed interface Label {
    data object NavigateBack : Label

    data class ShowToast(
      val message: String,
    ) : Label
  }
}
