package com.vireal.chordwizard.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.midi.core.MidiAvailability
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.midi.core.MidiScanState
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun SettingsScreen(
  appComponent: AppComponent,
  onNavigateBack: () -> Unit,
) {
  val store = remember { appComponent.settingsStoreProvider.create() }
  val state by store.stateFlow.collectAsState()

  LaunchedEffect(store) {
    store.labels.collectLatest { label ->
      when (label) {
        SettingsStore.Label.NavigateBack -> {
          onNavigateBack()
        }

        is SettingsStore.Label.ShowToast -> {
          // TODO: Show actual toast/snackbar
          println("Toast: ${label.message}")
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = {
            store.accept(SettingsStore.Intent.NavigateBack)
          }) {
            Text("←")
          }
        },
      )
    },
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
        ) {
          Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Dark Mode")
            Switch(
              checked = state.darkMode,
              onCheckedChange = {
                store.accept(SettingsStore.Intent.ToggleDarkMode)
              },
            )
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "MIDI Setup",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Connect your MIDI keyboard to play along.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          val isScanning = state.midiScanState is MidiScanState.Scanning
          Button(
            onClick = {
              if (isScanning) {
                store.accept(SettingsStore.Intent.StopMidiScan)
              } else {
                store.accept(SettingsStore.Intent.ScanForMidiDevices)
              }
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            if (isScanning) {
              Text("Stop Scan")
            } else {
              Text("Scan for MIDI Devices")
            }
          }

          Text(
            text = state.availabilityText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Available Devices",
            style = MaterialTheme.typography.titleMedium,
          )

          if (state.midiDevices.isEmpty()) {
            Text(
              text = "No devices found yet",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            state.midiDevices.forEach { device ->
              val isConnected =
                (state.midiConnectionState as? MidiConnectionState.Connected)
                  ?.device
                  ?.id == device.id

              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  ),
                tonalElevation = if (isConnected) 2.dp else 0.dp,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                  ) {
                    Text(
                      text = device.name ?: "Unnamed device",
                      style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                      text =
                        if (isConnected) {
                          "Connected"
                        } else {
                          "Tap Connect to start listening"
                        },
                      style = MaterialTheme.typography.bodySmall,
                      color =
                        if (isConnected) {
                          Color(0xFF1EAA5E)
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                  }

                  if (isConnected) {
                    OutlinedButton(
                      onClick = {
                        store.accept(SettingsStore.Intent.DisconnectMidiDevice)
                      },
                    ) {
                      Text("Disconnect")
                    }
                  } else {
                    Button(
                      onClick = {
                        store.accept(SettingsStore.Intent.ConnectMidiDevice(device.id))
                      },
                    ) {
                      Text("Connect")
                    }
                  }
                }
              }
            }
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
        ) {
          Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Enable Notifications")
            Switch(
              checked = state.notifications,
              onCheckedChange = {
                store.accept(SettingsStore.Intent.ToggleNotifications)
              },
            )
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
        ) {
          Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text("Version: ${state.version}")
          Text(state.buildInfo)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          store.accept(SettingsStore.Intent.ResetSettings)
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Reset to Defaults")
      }
    }
  }
}

private fun SettingsStore.State.availabilityText(): String =
  when (midiAvailability.status) {
    MidiAvailability.Status.AVAILABLE -> {
      when (midiScanState) {
        is MidiScanState.Scanning -> "Scanning nearby MIDI devices..."
        is MidiScanState.Failed -> "Scan failed. Check MIDI device connection."
        else -> "Ready to scan and connect."
      }
    }

    MidiAvailability.Status.UNSUPPORTED -> "MIDI is not supported on this device."
    MidiAvailability.Status.UNAVAILABLE -> midiAvailability.details ?: "MIDI is unavailable."
  }
