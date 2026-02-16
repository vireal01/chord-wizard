package com.vireal.chordwizard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.vireal.chordwizard.di.createAppComponent
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val appComponent = createAppComponent()

  ComposeViewport(document.body!!) {
    App(appComponent = appComponent)
  }
}