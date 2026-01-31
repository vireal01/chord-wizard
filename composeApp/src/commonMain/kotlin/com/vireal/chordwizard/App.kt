package com.vireal.chordwizard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.navigation.AppNavGraph

@Composable
@Preview
fun App(appComponent: AppComponent) {
    MaterialTheme {
        AppNavGraph(appComponent = appComponent)
    }
}
