package com.vireal.chordwizard

import androidx.compose.ui.window.ComposeUIViewController
import com.vireal.chordwizard.di.createAppComponent

fun MainViewController() =
  ComposeUIViewController {
    val appComponent = createAppComponent()
    App(appComponent = appComponent)
  }