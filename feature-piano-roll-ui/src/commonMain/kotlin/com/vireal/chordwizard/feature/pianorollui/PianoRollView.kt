package com.vireal.chordwizard.feature.pianorollui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PianoRollView(
  viewportState: PianoViewportState,
  pressedKeys: List<PressedKeyUi>,
  targetNotes: Set<Int>,
  modifier: Modifier = Modifier,
  showOctaveShifter: Boolean = false,
  onViewportAction: (PianoViewportAction) -> Unit = {},
  colors: PianoKeyboardColors = pianoKeyboardColors(),
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PianoKeyboardView(
      pressedKeys = pressedKeys,
      targetNotes = targetNotes,
      visibleRange = viewportState.visibleRange,
      colors = colors,
      modifier = Modifier.fillMaxWidth(),
    )

    if (showOctaveShifter) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Button(
          onClick = { onViewportAction(PianoViewportAction.ShiftLeft) },
          enabled = viewportState.canShiftLeft,
        ) {
          Text("< Octave")
        }

        Button(
          onClick = { onViewportAction(PianoViewportAction.ShiftRight) },
          enabled = viewportState.canShiftRight,
        ) {
          Text("Octave >")
        }
      }
    }
  }
}
