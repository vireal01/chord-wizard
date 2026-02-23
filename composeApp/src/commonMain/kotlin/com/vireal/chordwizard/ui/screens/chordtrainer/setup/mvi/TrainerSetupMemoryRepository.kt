package com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi

import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig

class TrainerSetupMemoryRepository {
  private var lastConfig: TrainerSessionConfig? = null

  fun get(): TrainerSessionConfig? = lastConfig

  fun save(config: TrainerSessionConfig) {
    lastConfig = config
  }
}
