package com.vireal.chordwizard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ChordWizard Color Palette (Material 3)
 * Custom color scheme optimized for music learning and chord visualization
 * Updated with vibrant blue accent colors
 */

// Primary Colors - Vibrant Blue
val Primary = Color(0xFF1A5CE5) // Main "Next Chord" button, active toggles
val OnPrimary = Color(0xFFFFFFFF) // White text on primary

// Secondary Colors - Deep Indigo
val Secondary = Color(0xFF0D2B6B) // Secondary button fills, subtle accents
val OnSecondary = Color(0xFFFFFFFF) // Text on secondary

// Tertiary Colors - Light Blue accent
val Tertiary = Color(0xFF4A90E2) // Creative accents, lighter blue
val OnTertiary = Color(0xFFFFFFFF) // Text on tertiary

// Background & Surface - Dark Navy
val Background = Color(0xFF0A1121) // Very dark navy/charcoal - app background
val OnBackground = Color(0xFFFFFFFF) // Pure white for headers and body text

val Surface = Color(0xFF141C2F) // Card surface (modal containers)
val OnSurface = Color(0xFFFFFFFF) // Primary text on surface
val SurfaceVariant = Color(0xFF1E2A42) // Slightly lighter surface variant
val OnSurfaceVariant = Color(0xFFA0A8B9) // Muted slate gray for secondary text

// Custom Feedback Colors
val CorrectNote = Color(0xFF4CAF50) // Success Green - correct pressed keys
val ErrorNote = Color(0xFFD32F2F) // Error Red - incorrect key highlight

// Piano Keyboard Colors
val PianoWhiteKey = Color(0xFFFFFFFF) // Pure white keys
val PianoBlackKey = Color(0xFF000000) // Pure black keys
val PianoKeyLabel = Color(0xFF7B8496) // Muted slate gray for note names

// Typography Colors
val TextPrimary = Color(0xFFFFFFFF) // Chord names, main button text
val TextSecondary = Color(0xFFA0A8B9) // Helper text, session progress labels

// Additional Material 3 colors
val Outline = Color(0xFF3A4557) // Borders, dividers
val OutlineVariant = Color(0xFF2A3545) // Subtle borders

// Container colors
val PrimaryContainer = Color(0xFF2B5FC7)
val OnPrimaryContainer = Color(0xFFD4E3FF)
val SecondaryContainer = Color(0xFF1A3A6B)
val OnSecondaryContainer = Color(0xFFB8C9E8)
val TertiaryContainer = Color(0xFF2D5A8F)
val OnTertiaryContainer = Color(0xFFD1E4FF)

val SurfaceContainerLowest = Color(0xFF050811)
val SurfaceContainerLow = Color(0xFF0A1121)
val SurfaceContainer = Color(0xFF0F1829)
val SurfaceContainerHigh = Color(0xFF141C2F)
val SurfaceContainerHighest = Color(0xFF1E2A42)

// Error colors
val Error = Color(0xFFD32F2F)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFF8C1D18)
val OnErrorContainer = Color(0xFFF9DEDC)

// Inverse colors
val InverseSurface = Color(0xFFE1E8F5)
val InverseOnSurface = Color(0xFF0A1121)
val InversePrimary = Color(0xFF1A5CE5)

// Scrim
val Scrim = Color(0xFF000000)

// Shadow for elevation (25-40% opacity)
val ShadowColor = Color(0x40000000) // 25% black
val ElevationShadow = Color(0x66000000) // 40% black