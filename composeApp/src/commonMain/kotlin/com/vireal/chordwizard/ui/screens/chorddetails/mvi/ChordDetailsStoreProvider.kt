package com.vireal.chordwizard.ui.screens.chorddetails.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.vireal.chordwizard.domain.model.ChordRoot
import me.tatarka.inject.annotations.Inject

@Inject
class ChordDetailsStoreProvider(
    private val storeFactory: StoreFactory
) {
    fun create(chordRoot: ChordRoot): ChordDetailsStore {
        return ChordDetailsStoreFactory(storeFactory, chordRoot).create()
    }
}
