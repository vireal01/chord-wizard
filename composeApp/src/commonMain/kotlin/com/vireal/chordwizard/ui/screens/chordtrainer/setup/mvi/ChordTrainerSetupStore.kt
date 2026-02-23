package com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi

import com.arkivanov.mvikotlin.core.store.Store
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerAdvanceMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerChordFamily
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerRoot
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig.Companion.DEFAULT_CARD_COUNT
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerMode
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore.Intent
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore.Label
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore.State

interface ChordTrainerSetupStore : Store<Intent, State, Label> {
  sealed interface Intent {
    data class ToggleFamily(
      val family: TrainerChordFamily,
    ) : Intent

    data class ToggleRoot(
      val root: TrainerRoot,
    ) : Intent

    data class SelectMode(
      val mode: TrainerMode,
    ) : Intent

    data class SelectAdvanceMode(
      val mode: TrainerAdvanceMode,
    ) : Intent

    data class SetCardCount(
      val count: Int,
    ) : Intent

    data object StartTraining : Intent

    data object NavigateBack : Intent
  }

  data class State(
    val availableFamilies: List<TrainerChordFamily> = TrainerChordFamily.entries,
    val availableRoots: List<TrainerRoot> = TrainerRoot.entries,
    val selectedFamilies: Set<TrainerChordFamily> = setOf(TrainerChordFamily.MAJOR),
    val selectedRoots: Set<TrainerRoot> = setOf(TrainerRoot.C),
    val selectedMode: TrainerMode = TrainerMode.Guided,
    val selectedAdvanceMode: TrainerAdvanceMode = TrainerAdvanceMode.Manual,
    val cardCount: Int = DEFAULT_CARD_COUNT,
    val isLoading: Boolean = false,
  ) {
    val startEnabled: Boolean
      get() = selectedFamilies.isNotEmpty() && selectedRoots.isNotEmpty()

    val poolSize: Int
      get() = selectedFamilies.size * selectedRoots.size

    val effectiveDeckSize: Int
      get() = minOf(cardCount.coerceAtLeast(1), poolSize)

    fun toSessionConfig(): TrainerSessionConfig =
      TrainerSessionConfig(
        selectedFamilies = selectedFamilies,
        selectedRoots = selectedRoots,
        mode = selectedMode,
        advanceMode = selectedAdvanceMode,
        cardCount = cardCount.coerceAtLeast(1),
      )
  }

  sealed interface Label {
    data class NavigateToSession(
      val config: TrainerSessionConfig,
    ) : Label

    data object NavigateBack : Label
  }
}
