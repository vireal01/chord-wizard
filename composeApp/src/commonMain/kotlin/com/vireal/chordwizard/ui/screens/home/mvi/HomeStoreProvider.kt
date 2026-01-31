package com.vireal.chordwizard.ui.screens.home.mvi

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.vireal.chordwizard.di.AppRepository
import me.tatarka.inject.annotations.Inject

/**
 * Factory to create HomeStore with DI
 */
@Inject
class HomeStoreProvider(
  private val storeFactory: StoreFactory,
  private val repository: AppRepository,
) {
  fun create(): HomeStore = HomeStoreFactory(storeFactory, repository).create()
}