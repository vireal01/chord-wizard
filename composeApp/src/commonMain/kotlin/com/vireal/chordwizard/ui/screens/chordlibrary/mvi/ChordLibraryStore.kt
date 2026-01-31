package com.vireal.chordwizard.ui.screens.chordlibrary.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore.Intent
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore.State
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore.Label

/**
 * ChordLibrary screen store interface
 * Displays only chord roots (C, D, E, F, G, A, B)
 * User can select variation in ChordDetails screen
 */
interface ChordLibraryStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data class SelectChordRoot(val chordRoot: ChordRoot) : Intent
        data object NavigateBack : Intent
    }

    data class State(
        val chordRoots: List<ChordRoot> = ChordRoot.entries,
        val isLoading: Boolean = false
    )

    sealed interface Label {
        data class NavigateToChordDetails(val chordRoot: ChordRoot) : Label
        data object NavigateBack : Label
    }
}
