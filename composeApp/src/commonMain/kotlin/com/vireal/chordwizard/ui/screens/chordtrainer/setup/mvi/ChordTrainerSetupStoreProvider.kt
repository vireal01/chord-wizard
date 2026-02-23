package com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import dev.zacsweers.metro.Inject

@Inject
class ChordTrainerSetupStoreProvider(
  private val storeFactory: StoreFactory,
  private val trainerSetupMemoryRepository: TrainerSetupMemoryRepository,
) {
  fun create(): ChordTrainerSetupStore =
    ChordTrainerSetupStoreFactory(
      storeFactory = storeFactory,
      trainerSetupMemoryRepository = trainerSetupMemoryRepository,
    ).create()
}
