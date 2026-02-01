package com.vireal.chordwizard.di

import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStoreProvider
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStoreProvider
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStoreProvider
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStoreProvider
import me.tatarka.inject.annotations.Component

/**
 * Main application DI component
 * Manages all application-level dependencies
 */
@Component
interface AppComponent : AppProvides {
  /**
   * Provides Repository instance
   */
  val repository: AppRepository

  /**
   * Provides ViewModel factory
   */
  val viewModel: () -> MainViewModel

  /**
   * Provides HomeStoreProvider for MVI
   */
  val homeStoreProvider: HomeStoreProvider

  /**
   * Provides ChordLibraryStoreProvider for MVI
   */
  val chordLibraryStoreProvider: ChordLibraryStoreProvider

  /**
   * Provides ChordDetailsStoreProvider for MVI
   */
  val chordDetailsStoreProvider: ChordDetailsStoreProvider

  /**
   * Provides SettingsStoreProvider for MVI
   */
  val settingsStoreProvider: SettingsStoreProvider
}