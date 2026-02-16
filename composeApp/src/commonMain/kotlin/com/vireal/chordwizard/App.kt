package com.vireal.chordwizard

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.navigation.AppNavGraph
import com.vireal.chordwizard.ui.theme.ChordWizardTheme

@Composable
@Preview
fun App(appComponent: AppComponent) {
  ChordWizardTheme {
    AppNavGraph(appComponent = appComponent)
  }
}