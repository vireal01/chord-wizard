package com.vireal.chordwizard.ui.screens.chordtrainer.session

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.vireal.chordwizard.di.AppComponent
import com.vireal.chordwizard.domain.model.NoteWithOctave
import com.vireal.chordwizard.feature.pianorollui.PianoValidationMode
import com.vireal.chordwizard.feature.pianorollui.PitchMatchMode
import com.vireal.chordwizard.midi.core.MidiConnectionState
import com.vireal.chordwizard.ui.components.PianoKeyboard
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStore
import com.vireal.chordwizard.ui.screens.home.mvi.ObserveActiveMidiNotesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChordTrainerSessionScreen(
  appComponent: AppComponent,
  config: TrainerSessionConfig,
  onNavigateBack: () -> Unit,
) {
  val store = remember(config) { appComponent.chordTrainerSessionStoreProvider.create(config) }
  val state by store.stateFlow.collectAsState()
  val midiInputService = appComponent.midiInputService
  val connectionState by midiInputService.connectionState.collectAsState(initial = MidiConnectionState.Disconnected)
  val activeMidiNotes by
    remember(midiInputService) {
      ObserveActiveMidiNotesUseCase(midiInputService)
        .execute()
        .map { notes -> notes.map { it.note }.toSet() }
    }.collectAsState(initial = emptySet())

  LaunchedEffect(store) {
    store.labels.collectLatest { label ->
      when (label) {
        ChordTrainerSessionStore.Label.NavigateBack -> onNavigateBack()
      }
    }
  }

  LaunchedEffect(activeMidiNotes) {
    store.accept(ChordTrainerSessionStore.Intent.UpdateMidiNotes(activeMidiNotes))
  }

  LaunchedEffect(midiInputService) {
    midiInputService.refreshAvailability()
    midiInputService.startScan()
  }

  LaunchedEffect(midiInputService) {
    midiInputService.discoveredDevices.collect { devices ->
      if (devices.isEmpty()) return@collect

      when (midiInputService.connectionState.value) {
        MidiConnectionState.Disconnected -> midiInputService.connect(devices.first().id)
        is MidiConnectionState.Failed -> midiInputService.connect(devices.first().id)
        else -> Unit
      }
    }
  }

  LaunchedEffect(state.currentIndex, state.isCurrentCorrect, state.shouldAutoAdvance, state.isFinished) {
    if (state.shouldAutoAdvance && !state.isFinished) {
      delay(600)
      store.accept(ChordTrainerSessionStore.Intent.NextChord)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Chord Trainer") },
        navigationIcon = {
          IconButton(onClick = { store.accept(ChordTrainerSessionStore.Intent.NavigateBack) }) {
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
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      if (state.isFinished) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("Session Complete", style = MaterialTheme.typography.headlineSmall)
            Text("Solved: ${state.solvedCount}")
            Text("Skipped: ${state.skippedCount}")
            Text("Mistakes: ${state.mistakesCount}")
            Text("Accuracy: ${state.accuracyPercent}%")
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onNavigateBack,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Back")
            }
          }
        }
        return@Column
      }

      val card = state.currentCard ?: return@Column

      Text(
        text = "Practice Mode",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(card.family.displayName.uppercase(), style = MaterialTheme.typography.labelMedium)
          Text(card.fullName, style = MaterialTheme.typography.headlineMedium)

          val notesText = card.targetNoteNames.joinToString(" - ")
          val helperText =
            when {
              !state.answerVisible -> "Play the chord and reveal when ready"
              state.notationEnabled -> "Play the notes: $notesText"
              else -> "Notation hidden"
            }

          Text(
            text = helperText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = state.notationEnabled,
          onClick = { store.accept(ChordTrainerSessionStore.Intent.ToggleNotation) },
          label = { Text(if (state.notationEnabled) "Notation On" else "Notation Off") },
        )
        FilterChip(
          selected = state.hintsEnabled,
          onClick = { store.accept(ChordTrainerSessionStore.Intent.ToggleHints) },
          label = { Text(if (state.hintsEnabled) "Hints On" else "Hints Off") },
        )
        if (!state.answerVisible) {
          TextButton(
            onClick = { store.accept(ChordTrainerSessionStore.Intent.RevealAnswer) },
          ) {
            Text("Reveal")
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PianoKeyboard(
            pressedNotes = state.activeNotes.toNoteWithOctaves(),
            targetSequence = state.targetSequence,
            trainingProgress = state.trainingProgress,
            validationMode = PianoValidationMode.ChordSet,
            pitchMatchMode = PitchMatchMode.PitchClass,
            showTargetDots = state.hintsEnabled,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Text(
        text = connectionState.toReadableStatus(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Text(
        text = "Session Progress ${state.progressCurrent}/${state.progressTotal}",
        style = MaterialTheme.typography.bodyMedium,
      )

      LinearProgressIndicator(
        progress = {
          if (state.progressTotal == 0) 0f else state.progressCurrent.toFloat() / state.progressTotal.toFloat()
        },
        modifier =
          Modifier
            .fillMaxWidth()
            .height(8.dp),
      )

      Button(
        onClick = { store.accept(ChordTrainerSessionStore.Intent.NextChord) },
        enabled = state.canAdvanceManually || state.shouldAutoAdvance,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp),
      ) {
        Text(if (state.shouldAutoAdvance) "Auto advancing..." else "Next Chord")
      }

      OutlinedButton(
        onClick = { store.accept(ChordTrainerSessionStore.Intent.SkipChord) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Skip")
      }
    }
  }
}

private fun Set<Int>.toNoteWithOctaves(): List<NoteWithOctave> =
  map { midi ->
    val absolute = (midi - 12).coerceAtLeast(0)
    NoteWithOctave.fromAbsolutePosition(absolute)
  }.sortedBy { it.absolutePosition }

private fun MidiConnectionState.toReadableStatus(): String =
  when (this) {
    MidiConnectionState.Disconnected -> "MIDI: Disconnected"
    is MidiConnectionState.Connecting -> "MIDI: Connecting..."
    is MidiConnectionState.Connected -> "MIDI: Connected (${device.transport.name})"
    is MidiConnectionState.Disconnecting -> "MIDI: Disconnecting..."
    is MidiConnectionState.Failed -> "MIDI: Failed (${error.message})"
  }
