package com.vireal.chordwizard

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.di.create

fun main() =
  application {
    val appComponent = AppComponent::class.create()

    Window(
      onCloseRequest = ::exitApplication,
      title = "ChordWizard",
    ) {
      App(appComponent = appComponent)
    }
  }