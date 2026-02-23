package com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import dev.zacsweers.metro.Inject

@Inject
class ChordTrainerSessionStoreProvider(
  private val storeFactory: StoreFactory,
) {
  fun create(config: TrainerSessionConfig): ChordTrainerSessionStore =
    ChordTrainerSessionStoreFactory(
      storeFactory = storeFactory,
      config = config,
    ).create()
}
