package com.vireal.chordwizard.navigation

import com.vireal.chordwizard.domain.model.ChordRoot
import kotlinx.serialization.Serializable

/**
 * Navigation routes for the app
 * Using type-safe navigation with serialization
 */
sealed interface Route {
  @Serializable
  data object Home : Route

  @Serializable
  data object ChordLibrary : Route

  @Serializable
  data class ChordDetails(
    val chordRoot: ChordRoot,
  ) : Route

  @Serializable
  data object Settings : Route
}