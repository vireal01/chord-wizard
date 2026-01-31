package com.vireal.chordwizard.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
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