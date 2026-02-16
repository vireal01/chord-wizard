package com.vireal.chordwizard

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.di.createAppComponent

fun main() =
  application {
    val appComponent = createAppComponent()

    Window(
      onCloseRequest = ::exitApplication,
      title = "ChordWizard",
    ) {
      App(appComponent = appComponent)
    }
  }