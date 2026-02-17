package com.vireal.chordwizard.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun HomeScreen(
  appComponent: AppComponent,
  onNavigateToChordLibrary: () -> Unit,
  onNavigateToSettings: () -> Unit,
) {
  // Create store using DI
  val store = remember { appComponent.homeStoreProvider.create() }

  // Collect state as StateFlow
  val state by store.stateFlow.collectAsState()

  // Handle side effects (labels)
  LaunchedEffect(store) {
    store.labels.collectLatest { label ->
      when (label) {
        HomeStore.Label.NavigateToChordLibrary -> onNavigateToChordLibrary()
        HomeStore.Label.NavigateToSettings -> onNavigateToSettings()
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("ChordWizard") },
        actions = {
          IconButton(onClick = {
            store.accept(HomeStore.Intent.NavigateToSettings)
          }) {
            Text("⚙️")
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
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = state.greeting,
        style = MaterialTheme.typography.headlineMedium,
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = state.appInfo,
        style = MaterialTheme.typography.bodyLarge,
      )

      Spacer(modifier = Modifier.height(48.dp))

      Button(
        onClick = { store.accept(HomeStore.Intent.ToggleContent) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Toggle Content")
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = { store.accept(HomeStore.Intent.NavigateToChordLibrary) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Open Chord Library")
      }

      Spacer(modifier = Modifier.height(24.dp))
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Live MIDI Notes",
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(8.dp))
          if (state.activeMidiNotes.isEmpty()) {
            Text(
              text = "No active notes",
              style = MaterialTheme.typography.bodyMedium,
            )
          } else {
            Text(
              text = state.activeMidiNotes.joinToString { "${it.noteName}(ch${it.channel + 1})" },
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }

      if (state.showContent) {
        Spacer(modifier = Modifier.height(24.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Content is now visible! (MVI State)",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}
