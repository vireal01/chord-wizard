package com.vireal.chordwizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.di.create

class MainActivity : ComponentActivity() {

    private val appComponent by lazy { AppComponent::class.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(appComponent = appComponent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val appComponent = AppComponent::class.create()
    App(appComponent = appComponent)
}
