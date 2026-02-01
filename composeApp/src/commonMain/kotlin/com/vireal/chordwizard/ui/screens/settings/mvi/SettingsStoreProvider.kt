package com.vireal.chordwizard.ui.screens.settings.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsStoreProvider(
  private val storeFactory: StoreFactory,
) {
  fun create(): SettingsStore = SettingsStoreFactory(storeFactory).create()
}