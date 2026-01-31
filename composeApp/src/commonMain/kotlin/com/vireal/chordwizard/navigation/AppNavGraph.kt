package com.vireal.chordwizard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.ui.screens.chorddetails.ChordDetailsScreen
import com.vireal.chordwizard.ui.screens.chordlibrary.ChordLibraryScreen
import com.vireal.chordwizard.ui.screens.home.HomeScreen
import com.vireal.chordwizard.ui.screens.settings.SettingsScreen

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
      )
    }

    composable<Route.ChordLibrary> {
      ChordLibraryScreen(
        appComponent = appComponent,
        onNavigateToChordDetails = { chordRoot ->
          navController.navigate(Route.ChordDetails(chordRoot))
        },
        onNavigateBack = {
          navController.popBackStack()
        },
      )
    }

    composable<Route.ChordDetails> { backStackEntry ->
      val args = backStackEntry.toRoute<Route.ChordDetails>()
      ChordDetailsScreen(
        appComponent = appComponent,
        chordRoot = args.chordRoot,
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
  }
}