package com.vireal.chordwizard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.feature.pianorollui.NoteVisualizerScreen
import com.vireal.chordwizard.feature.pianorollui.pianoKeyboardColors
import com.vireal.chordwizard.ui.screens.chorddetails.ChordDetailsScreen
import com.vireal.chordwizard.ui.screens.chordlibrary.ChordLibraryScreen
import com.vireal.chordwizard.ui.screens.home.HomeScreen
import com.vireal.chordwizard.ui.screens.settings.SettingsScreen
import com.vireal.chordwizard.ui.theme.CorrectNote
import com.vireal.chordwizard.ui.theme.ErrorNote

/**
 * Main navigation graph for the application
 */
@Composable
fun AppNavGraph(
  appComponent: AppComponent,
  navController: NavHostController = rememberNavController(),
) {
  NavHost(
    navController = navController,
    startDestination = Route.Home,
  ) {
    composable<Route.Home> {
      HomeScreen(
        appComponent = appComponent,
        onNavigateToChordLibrary = {
          navController.navigate(Route.ChordLibrary)
        },
        onNavigateToSettings = {
          navController.navigate(Route.Settings)
        },
        onNavigateToNoteVisualizer = {
          navController.navigate(Route.NoteVisualizer)
        },
      )
    }

    composable<Route.ChordLibrary> {
      ChordLibraryScreen(
        appComponent = appComponent,
        onNavigateToChordDetails = { chordRoot ->
          navController.navigate(Route.ChordDetails(chordRoot.name))
        },
        onNavigateBack = {
          navController.popBackStack()
        },
      )
    }

    composable<Route.ChordDetails> { backStackEntry ->
      val args = backStackEntry.toRoute<Route.ChordDetails>()
      val chordRoot = ChordRoot.valueOf(args.chordRootName)
      ChordDetailsScreen(
        appComponent = appComponent,
        chordRoot = chordRoot,
        onNavigateBack = {
          navController.popBackStack()
        },
      )
    }

    composable<Route.Settings> {
      SettingsScreen(
        appComponent = appComponent,
        onNavigateBack = {
          navController.popBackStack()
        },
      )
    }

    composable<Route.NoteVisualizer> {
      NoteVisualizerScreen(
        midiInputService = appComponent.midiInputService,
        onNavigateBack = {
          navController.popBackStack()
        },
        colors =
          pianoKeyboardColors(
            targetGlow = CorrectNote,
            wrongGlow = ErrorNote,
          ),
      )
    }
  }
}
