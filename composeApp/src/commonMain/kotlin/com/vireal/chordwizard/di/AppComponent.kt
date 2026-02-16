package com.vireal.chordwizard.di

import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStoreProvider
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStoreProvider
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStoreProvider
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStoreProvider
import dev.zacsweers.metro.DependencyGraph

/**
 * Main application DI component
 * Manages all application-level dependencies
 */
@DependencyGraph
abstract class AppComponent : AppProvides {
  /**
   * Provides Repository instance
   */
  abstract val repository: AppRepository

  /**
   * Provides HomeStoreProvider for MVI
   */
  abstract val homeStoreProvider: HomeStoreProvider

  /**
   * Provides ChordLibraryStoreProvider for MVI
   */
  abstract val chordLibraryStoreProvider: ChordLibraryStoreProvider

  /**
   * Provides ChordDetailsStoreProvider for MVI
   */
  abstract val chordDetailsStoreProvider: ChordDetailsStoreProvider

  /**
   * Provides SettingsStoreProvider for MVI
   */
  abstract val settingsStoreProvider: SettingsStoreProvider
}
