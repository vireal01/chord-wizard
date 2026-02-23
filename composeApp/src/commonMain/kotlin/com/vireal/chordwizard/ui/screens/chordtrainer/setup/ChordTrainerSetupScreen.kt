package com.vireal.chordwizard.ui.screens.chordtrainer.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest

private val CARD_COUNT_OPTIONS = listOf(5, 10, 20)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChordTrainerSetupScreen(
  appComponent: AppComponent,
  onNavigateBack: () -> Unit,
  onNavigateToSession: (TrainerSessionConfig) -> Unit,
) {
  val store = remember { appComponent.chordTrainerSetupStoreProvider.create() }
  val state by store.stateFlow.collectAsState()

  LaunchedEffect(store) {
    store.labels.collectLatest { label ->
      when (label) {
        ChordTrainerSetupStore.Label.NavigateBack -> onNavigateBack()
        is ChordTrainerSetupStore.Label.NavigateToSession -> onNavigateToSession(label.config)
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Chord Trainer Setup") },
        navigationIcon = {
          IconButton(
            onClick = { store.accept(ChordTrainerSetupStore.Intent.NavigateBack) },
          ) {
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
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Chord Families", style = MaterialTheme.typography.titleMedium)
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            state.availableFamilies.forEach { family ->
              FilterChip(
                selected = family in state.selectedFamilies,
                onClick = { store.accept(ChordTrainerSetupStore.Intent.ToggleFamily(family)) },
                label = { Text(family.displayName) },
              )
            }
          }
        }
      }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Roots", style = MaterialTheme.typography.titleMedium)
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            state.availableRoots.forEach { root ->
              FilterChip(
                selected = root in state.selectedRoots,
                onClick = { store.accept(ChordTrainerSetupStore.Intent.ToggleRoot(root)) },
                label = { Text(root.displayName) },
              )
            }
          }
        }
      }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Mode", style = MaterialTheme.typography.titleMedium)
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TrainerMode.entries.forEach { mode ->
              FilterChip(
                selected = mode == state.selectedMode,
                onClick = { store.accept(ChordTrainerSetupStore.Intent.SelectMode(mode)) },
                label = { Text(mode.name) },
              )
            }
          }

          Text("Advance", style = MaterialTheme.typography.titleMedium)
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerAdvanceMode.entries.forEach { mode ->
              FilterChip(
                selected = mode == state.selectedAdvanceMode,
                onClick = { store.accept(ChordTrainerSetupStore.Intent.SelectAdvanceMode(mode)) },
                label = { Text(mode.name) },
              )
            }
          }

          Text("Card Count", style = MaterialTheme.typography.titleMedium)
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CARD_COUNT_OPTIONS.forEach { count ->
              FilterChip(
                selected = count == state.cardCount,
                onClick = { store.accept(ChordTrainerSetupStore.Intent.SetCardCount(count)) },
                label = { Text(count.toString()) },
              )
            }
          }
        }
      }

      Text(
        text = "Pool size: ${state.poolSize} chords, session deck: ${state.effectiveDeckSize}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = { store.accept(ChordTrainerSetupStore.Intent.StartTraining) },
        enabled = state.startEnabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp),
      ) {
        Text("Start Training")
      }
    }
  }
}