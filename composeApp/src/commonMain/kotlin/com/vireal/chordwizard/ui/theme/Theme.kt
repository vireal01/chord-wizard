package com.vireal.chordwizard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * ChordWizard Material 3 Theme
 *
 * Features:
 * - Custom color palette optimized for music learning
 * - High contrast for visual feedback (correct/error notes)
 * - Dark theme optimized for extended use
 * - System typography with monospace for technical notation
 */
@Composable
fun ChordWizardTheme(
    @Suppress("UNUSED_PARAMETER")
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use dark theme for now (designed for dark mode)
    val colorScheme = ChordWizardColorScheme
    val typography = ChordWizardTypography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

/**
 * Extension properties for accessing custom colors
 * Use these for special cases like correct/error note feedback
 */
object ChordWizardColors {
    val correctNote = CorrectNote
    val errorNote = ErrorNote
}
