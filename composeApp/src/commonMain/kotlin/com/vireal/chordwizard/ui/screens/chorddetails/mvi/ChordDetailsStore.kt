package com.vireal.chordwizard.ui.screens.chorddetails.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.domain.builder.ChordBuilder
import com.vireal.chordwizard.domain.model.Chord
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.domain.model.ChordType
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore.Intent
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore.State
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore.Label

/**
 * ChordDetails screen store interface
 */
interface ChordDetailsStore : Store<Intent, State, Label> {

    sealed interface Intent {
        data object NavigateBack : Intent
        data object PlayChord : Intent
        data object ToggleFavorite : Intent
        data class SelectChordType(val chordType: ChordType) : Intent
    }

    data class State(
        val chordRoot: ChordRoot,
        val selectedChordType: ChordType = ChordType.MAJOR,
        val availableChordTypes: List<ChordType> = ChordType.entries,
        val isFavorite: Boolean = false,
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false
    ) {
        val currentChord: Chord
            get() = Chord(chordRoot, selectedChordType)

        val chordDisplayName: String
            get() = currentChord.displayName

        val chordFullName: String
            get() = currentChord.fullName

        /**
         * Get chord notes as string (e.g., "C - E - G")
         */
        val chordNotes: String
            get() = ChordBuilder.buildChordString(currentChord)

        /**
         * Get chord difficulty
         */
        val chordDifficulty: String
            get() = ChordBuilder.getChordDifficulty(currentChord)

        /**
         * Get finger position
         */
        val fingerPosition: String
            get() = ChordBuilder.getFingerPosition(currentChord)
    }

    sealed interface Label {
        data object NavigateBack : Label
        data class ShowToast(val message: String) : Label
    }
}
