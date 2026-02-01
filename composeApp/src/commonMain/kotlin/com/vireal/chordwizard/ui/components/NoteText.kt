package com.vireal.chordwizard.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.vireal.chordwizard.ui.theme.MonospaceTextStyle

/**
 * Text component for displaying technical music notation
 * Uses monospace font for precise, musical tool feel
 *
 * Examples: "C4", "E4", "G4", "A#4", "D5"
 */
@Composable
fun NoteText(
  note: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = note,
    style = MonospaceTextStyle,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = modifier,
  )
}

/**
 * Text component for displaying chord notes sequence
 * Example: "C4 - E4 - G4 - A#4"
 */
@Composable
fun ChordNotesText(
  notes: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = notes,
    style =
      MonospaceTextStyle.copy(
        fontFamily = FontFamily.Monospace,
      ),
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier,
  )
}