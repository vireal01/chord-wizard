package com.vireal.chordwizard.ui.screens.chordlibrary.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import dev.zacsweers.metro.Inject

@Inject
class ChordLibraryStoreProvider(
  private val storeFactory: StoreFactory,
) {
  fun create(): ChordLibraryStore = ChordLibraryStoreFactory(storeFactory).create()
}