package com.vireal.chordwizard.ui.screens.home.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.vireal.chordwizard.di.AppRepository
import com.vireal.chordwizard.midi.core.MidiInputService
import dev.zacsweers.metro.Inject

/**
 * Factory to create HomeStore with DI
 */
@Inject
class HomeStoreProvider(
  private val storeFactory: StoreFactory,
  private val repository: AppRepository,
  private val midiInputService: MidiInputService,
) {
  fun create(): HomeStore =
    HomeStoreFactory(
      storeFactory = storeFactory,
      repository = repository,
      midiInputService = midiInputService,
      observeActiveMidiNotesUseCase = ObserveActiveMidiNotesUseCase(midiInputService),
    ).create()
}
