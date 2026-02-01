package com.vireal.chordwizard.domain.builder

import com.vireal.chordwizard.domain.model.Chord
import com.vireal.chordwizard.domain.model.ChordType
import com.vireal.chordwizard.domain.model.Note
import com.vireal.chordwizard.domain.model.NoteWithOctave

/**
 * Builds chord notes based on music theory
 */
object ChordBuilder {
  /**
   * Get notes for a chord (without octave information)
   * @deprecated Use buildChordWithOctaves for accurate octave representation
   */
  fun buildChord(chord: Chord): List<Note> {
    val root = Note.fromChordRoot(chord.root)
    val intervals = getIntervals(chord.type)

    return intervals.map { interval ->
      root + interval
    }
  }

  /**
   * Get notes for a chord with octave information
   * Starting from octave 4 (middle C)
   */
  fun buildChordWithOctaves(
    chord: Chord,
    startOctave: Int = 4,
  ): List<NoteWithOctave> {
    val root = NoteWithOctave(Note.fromChordRoot(chord.root), startOctave)
    val intervals = getIntervals(chord.type)

    return intervals.map { interval ->
      root + interval
    }
  }

  /**
   * Get notes as formatted string (e.g., "C - E - G")
   */
  fun buildChordString(chord: Chord): String = buildChord(chord).joinToString(" - ") { it.displayName }

  /**
   * Get notes with octaves as formatted string (e.g., "C4 - E4 - G4 - A#4 - D5")
   */
  fun buildChordStringWithOctaves(
    chord: Chord,
    startOctave: Int = 4,
  ): String = buildChordWithOctaves(chord, startOctave).joinToString(" - ") { it.displayName }

  /**
   * Get intervals (semitones from root) for each chord type
   */
  private fun getIntervals(type: ChordType): List<Int> =
    when (type) {
      // Major: Root, Major 3rd, Perfect 5th
      ChordType.MAJOR -> listOf(0, 4, 7)

      // Minor: Root, Minor 3rd, Perfect 5th
      ChordType.MINOR -> listOf(0, 3, 7)

      // Seventh: Root, Major 3rd, Perfect 5th, Minor 7th
      ChordType.SEVENTH -> listOf(0, 4, 7, 10)

      // Major 7th: Root, Major 3rd, Perfect 5th, Major 7th
      ChordType.MAJOR_SEVENTH -> listOf(0, 4, 7, 11)

      // Minor 7th: Root, Minor 3rd, Perfect 5th, Minor 7th
      ChordType.MINOR_SEVENTH -> listOf(0, 3, 7, 10)

      // Diminished: Root, Minor 3rd, Diminished 5th
      ChordType.DIMINISHED -> listOf(0, 3, 6)

      // Augmented: Root, Major 3rd, Augmented 5th
      ChordType.AUGMENTED -> listOf(0, 4, 8)

      // Suspended 2nd: Root, Major 2nd, Perfect 5th
      ChordType.SUSPENDED_2 -> listOf(0, 2, 7)

      // Suspended 4th: Root, Perfect 4th, Perfect 5th
      ChordType.SUSPENDED_4 -> listOf(0, 5, 7)

      // Sixth: Root, Major 3rd, Perfect 5th, Major 6th
      ChordType.SIXTH -> listOf(0, 4, 7, 9)

      // Ninth: Root, Major 3rd, Perfect 5th, Minor 7th, Major 9th
      // 9th = octave (12) + major 2nd (2) = 14 semitones
      ChordType.NINTH -> listOf(0, 4, 7, 10, 14)
    }

  /**
   * Get chord difficulty based on number of notes and complexity
   */
  fun getChordDifficulty(chord: Chord): String {
    val noteCount = buildChord(chord).size
    return when {
      noteCount <= 3 && chord.type in listOf(ChordType.MAJOR, ChordType.MINOR) -> "Beginner"
      noteCount <= 4 && chord.type !in listOf(ChordType.AUGMENTED, ChordType.DIMINISHED) -> "Intermediate"
      else -> "Advanced"
    }
  }

  /**
   * Get suggested finger position (simplified)
   */
  fun getFingerPosition(chord: Chord): String =
    when (chord.type) {
      ChordType.MAJOR -> "1-2-3"
      ChordType.MINOR -> "1-2-3"
      ChordType.SEVENTH -> "1-2-3-4"
      ChordType.MAJOR_SEVENTH -> "1-2-3-4"
      ChordType.MINOR_SEVENTH -> "1-2-3-4"
      ChordType.DIMINISHED -> "1-2-3"
      ChordType.AUGMENTED -> "1-2-3"
      ChordType.SUSPENDED_2 -> "1-3-4"
      ChordType.SUSPENDED_4 -> "1-3-4"
      ChordType.SIXTH -> "1-2-3-4"
      ChordType.NINTH -> "1-2-3-4-5"
    }

  /**
   * Calculate how many octaves are required to display this chord
   * Returns 1 if all notes fit in one octave, 2 if they span two octaves, etc.
   *
   * Note: PianoKeyboard now automatically displays up to the last note,
   * so this method is kept mainly for testing and reference purposes.
   */
  fun getRequiredOctaveCount(
    chord: Chord,
    startOctave: Int = 4,
  ): Int {
    val notesWithOctaves = buildChordWithOctaves(chord, startOctave)

    if (notesWithOctaves.isEmpty()) return 1

    val minOctave = notesWithOctaves.minOf { it.octave }
    val maxOctave = notesWithOctaves.maxOf { it.octave }

    return maxOctave - minOctave + 1
  }
}