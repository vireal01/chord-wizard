package com.vireal.chordwizard.ui.screens.chorddetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.domain.builder.ChordBuilder
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.ui.components.GuitarChordDiagram
import com.vireal.chordwizard.ui.components.PianoKeyboard
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ChordDetailsScreen(
  appComponent: AppComponent,
  chordRoot: ChordRoot,
  onNavigateBack: () -> Unit,
) {
  val store = remember { appComponent.chordDetailsStoreProvider.create(chordRoot) }
  val state by store.stateFlow.collectAsStateWithLifecycle()

  LaunchedEffect(store) {
    store.labels.collectLatest { label ->
      when (label) {
        ChordDetailsStore.Label.NavigateBack -> {
          onNavigateBack()
        }

        is ChordDetailsStore.Label.ShowToast -> {
          // TODO: Show actual toast/snackbar
          println("Toast: ${label.message}")
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(state.chordDisplayName) },
        navigationIcon = {
          IconButton(onClick = {
            store.accept(ChordDetailsStore.Intent.NavigateBack)
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
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Chord Type Selector
      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Chord Variations",
            style = MaterialTheme.typography.titleMedium,
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.availableChordTypes) { chordType ->
              FilterChip(
                selected = chordType == state.selectedChordType,
                onClick = {
                  store.accept(ChordDetailsStore.Intent.SelectChordType(chordType))
                },
                label = { Text(chordType.displayName) },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Chord: ${state.chordDisplayName}",
              style = MaterialTheme.typography.titleLarge,
            )
            IconButton(
              onClick = {
                store.accept(ChordDetailsStore.Intent.ToggleFavorite)
              },
            ) {
              Text(if (state.isFavorite) "❤️" else "🤍")
            }
          }
          HorizontalDivider()

          // Display calculated notes
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Notes: ",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = state.chordNotes,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          Text("Difficulty: ${state.chordDifficulty}")
          Text("Finger Position: ${state.fingerPosition}")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Piano Keyboard
      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Piano",
            style = MaterialTheme.typography.titleMedium,
          )
          PianoKeyboard(
            pressedNotes = ChordBuilder.buildChordWithOctaves(state.currentChord),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      // Guitar Chord Diagram
      Card(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          GuitarChordDiagram(
            chord = state.currentChord,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = {
          store.accept(ChordDetailsStore.Intent.PlayChord)
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isPlaying,
      ) {
        Text(if (state.isPlaying) "Playing..." else "Play ${state.chordDisplayName}")
      }

      Button(
        onClick = {
          store.accept(ChordDetailsStore.Intent.NavigateBack)
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Back to Library")
      }
    }
  }
}