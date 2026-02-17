package com.vireal.chordwizard.ui.screens.settings.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.vireal.chordwizard.midi.core.MidiInputService
import dev.zacsweers.metro.Inject

@Inject
class SettingsStoreProvider(
  private val storeFactory: StoreFactory,
  private val midiInputService: MidiInputService,
) {
  fun create(): SettingsStore = SettingsStoreFactory(storeFactory, midiInputService).create()
}
