package com.vireal.chordwizard.ui.screens.chordlibrary.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import me.tatarka.inject.annotations.Inject

@Inject
class ChordLibraryStoreProvider(
    private val storeFactory: StoreFactory
) {
    fun create(): ChordLibraryStore {
        return ChordLibraryStoreFactory(storeFactory).create()
    }
}
