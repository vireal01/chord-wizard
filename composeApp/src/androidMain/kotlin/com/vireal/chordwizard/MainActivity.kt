package com.vireal.chordwizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vireal.chordwizard.bluetoothmidi.AndroidBluetoothMidiRuntime
import com.vireal.chordwizard.di.createAppComponent
import com.vireal.chordwizard.midi.usb.AndroidUsbMidiRuntime

class MainActivity : ComponentActivity() {
  private val appComponent by lazy { createAppComponent() }

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidBluetoothMidiRuntime.initialize(applicationContext)
    AndroidUsbMidiRuntime.initialize(applicationContext)
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
  val appComponent = createAppComponent()
  App(appComponent = appComponent)
}
