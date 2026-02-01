package com.vireal.chordwizard.ui.screens.chordlibrary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ChordLibraryScreen(
  appComponent: AppComponent,
  onNavigateToChordDetails: (ChordRoot) -> Unit,
  onNavigateBack: () -> Unit,
) {
  val store = remember { appComponent.chordLibraryStoreProvider.create() }
  val state by store.stateFlow.collectAsState()

  LaunchedEffect(store) {
    store.labels.collectLatest { label ->
      when (label) {
        is ChordLibraryStore.Label.NavigateToChordDetails -> {
          onNavigateToChordDetails(label.chordRoot)
        }

        ChordLibraryStore.Label.NavigateBack -> {
          onNavigateBack()
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Chord Library") },
        navigationIcon = {
          IconButton(onClick = {
            store.accept(ChordLibraryStore.Intent.NavigateBack)
          }) {
            Text("←")
          }
        },
      )
    },
  ) { paddingValues ->
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text(
          text = "Select a chord root",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      }

      items(state.chordRoots) { chordRoot ->
        Card(
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable {
                store.accept(ChordLibraryStore.Intent.SelectChordRoot(chordRoot))
              },
        ) {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${chordRoot.displayName} Major",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "→",
              style = MaterialTheme.typography.titleLarge,
            )
          }
        }
      }
    }
  }
}