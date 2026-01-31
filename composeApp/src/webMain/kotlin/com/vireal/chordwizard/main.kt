package com.vireal.chordwizard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.di.create

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val appComponent = AppComponent::class.create()

    ComposeViewport {
        App(appComponent = appComponent)
    }
}

