package com.vireal.chordwizard.feature.pianorollui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiInputService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteVisualizerScreen(
  midiInputService: MidiInputService,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  targetNotes: Set<Int> = emptySet(),
  visibleRange: IntRange = 36..96,
  colors: PianoKeyboardColors = pianoKeyboardColors(),
) {
  val screenScope = rememberCoroutineScope()
  val pressedKeysFlow = remember(midiInputService) { midiInputService.noteEvents.trackPressedKeys() }
  val pressedKeys by pressedKeysFlow.collectAsState(initial = emptyList())
  val connectionState by midiInputService.connectionState.collectAsState(initial = MidiConnectionState.Disconnected)
  val connectedDeviceName by
    midiInputService.connectionState
      .map { state -> (state as? MidiConnectionState.Connected)?.device?.name ?: "No device connected" }
      .collectAsState(initial = "No device connected")

  LaunchedEffect(midiInputService) {
    midiInputService.refreshAvailability()
    midiInputService.startScan()
  }

  LaunchedEffect(midiInputService) {
    midiInputService.discoveredDevices.collect { devices ->
      if (devices.isEmpty()) return@collect
      val firstDeviceId = devices.first().id
      when (val connection = midiInputService.connectionState.value) {
        MidiConnectionState.Disconnected -> {
          midiInputService.connect(firstDeviceId)
        }

        is MidiConnectionState.Failed -> {
          midiInputService.connect(firstDeviceId)
        }

        is MidiConnectionState.Connecting -> {
          if (connection.target.id !=
            firstDeviceId
          ) {
            midiInputService.connect(firstDeviceId)
          }
        }

        is MidiConnectionState.Connected -> {
          if (connection.device.id !=
            firstDeviceId
          ) {
            midiInputService.connect(firstDeviceId)
          }
        }

        is MidiConnectionState.Disconnecting -> {
          Unit
        }
      }
    }
  }

  DisposableEffect(midiInputService) {
    onDispose {
      screenScope.launch {
        midiInputService.stopScan()
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Note Visualizer") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Text("←")
          }
        },
      )
    },
    modifier = modifier,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = connectedDeviceName,
        style = MaterialTheme.typography.titleMedium,
      )

      Text(
        text = connectionState.toReadableStatus(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(8.dp))

      PianoKeyboardView(
        pressedKeys = pressedKeys,
        targetNotes = targetNotes,
        visibleRange = visibleRange,
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

private fun MidiConnectionState.toReadableStatus(): String =
  when (this) {
    MidiConnectionState.Disconnected -> "Disconnected"
    is MidiConnectionState.Connecting -> "Connecting to ${target.name ?: target.id}..."
    is MidiConnectionState.Connected -> "Connected (${device.transport.name})"
    is MidiConnectionState.Disconnecting -> "Disconnecting..."
    is MidiConnectionState.Failed -> "Connection failed: ${error.message}"
  }