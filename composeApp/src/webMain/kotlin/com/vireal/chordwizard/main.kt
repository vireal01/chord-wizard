package com.vireal.chordwizard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.di.create
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val appComponent = AppComponent::class.create()

  ComposeViewport(document.body!!) {
    App(appComponent = appComponent)
  }
}