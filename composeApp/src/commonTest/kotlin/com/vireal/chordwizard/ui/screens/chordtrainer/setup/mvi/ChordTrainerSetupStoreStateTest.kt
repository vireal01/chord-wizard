package com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi

import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerChordFamily
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerRoot
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordTrainerSetupStoreStateTest {
  @Test
  fun `start is disabled when no families selected`() {
    val state =
      ChordTrainerSetupStore.State(
        selectedFamilies = emptySet(),
        selectedRoots = setOf(TrainerRoot.C),
      )

    assertFalse(state.startEnabled)
  }

  @Test
  fun `start is disabled when no roots selected`() {
    val state =
      ChordTrainerSetupStore.State(
        selectedFamilies = setOf(TrainerChordFamily.MAJOR),
        selectedRoots = emptySet(),
      )

    assertFalse(state.startEnabled)
  }

  @Test
  fun `start is enabled when families and roots are selected`() {
    val state =
      ChordTrainerSetupStore.State(
        selectedFamilies = setOf(TrainerChordFamily.MAJOR, TrainerChordFamily.MINOR),
        selectedRoots = setOf(TrainerRoot.C, TrainerRoot.D),
      )

    assertTrue(state.startEnabled)
  }
}
