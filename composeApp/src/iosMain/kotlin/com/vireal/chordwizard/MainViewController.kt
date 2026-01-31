package com.vireal.chordwizard

import androidx.compose.ui.window.ComposeUIViewController
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.di.create

fun MainViewController() = ComposeUIViewController {
    val appComponent = AppComponent::class.create()
    App(appComponent = appComponent)
}
