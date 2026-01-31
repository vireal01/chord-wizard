package com.vireal.chordwizard.domain.builder

import com.vireal.chordwizard.domain.model.Chord
import com.vireal.chordwizard.domain.model.ChordRoot
import com.vireal.chordwizard.domain.model.ChordType
import com.vireal.chordwizard.domain.model.Note
import kotlin.test.Test
import kotlin.test.assertEquals

class ChordBuilderTest {
  @Test
  fun `C Major chord should be C E G`() {
    val chord = Chord(ChordRoot.C, ChordType.MAJOR)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.C, notes[0])
    assertEquals(Note.E, notes[1])
    assertEquals(Note.G, notes[2])

    assertEquals("C - E - G", ChordBuilder.buildChordString(chord))
  }

  @Test
  fun `C Minor chord should be C D# G`() {
    val chord = Chord(ChordRoot.C, ChordType.MINOR)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.C, notes[0])
    assertEquals(Note.D_SHARP, notes[1])
    assertEquals(Note.G, notes[2])

    assertEquals("C - D# - G", ChordBuilder.buildChordString(chord))
  }

  @Test
  fun `D Major chord should be D F# A`() {
    val chord = Chord(ChordRoot.D, ChordType.MAJOR)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.D, notes[0])
    assertEquals(Note.F_SHARP, notes[1])
    assertEquals(Note.A, notes[2])

    assertEquals("D - F# - A", ChordBuilder.buildChordString(chord))
  }

  @Test
  fun `G Seventh chord should be G B D F`() {
    val chord = Chord(ChordRoot.G, ChordType.SEVENTH)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(4, notes.size)
    assertEquals(Note.G, notes[0])
    assertEquals(Note.B, notes[1])
    assertEquals(Note.D, notes[2])
    assertEquals(Note.F, notes[3])

    assertEquals("G - B - D - F", ChordBuilder.buildChordString(chord))
  }

  @Test
  fun `A Minor chord should be A C E`() {
    val chord = Chord(ChordRoot.A, ChordType.MINOR)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.A, notes[0])
    assertEquals(Note.C, notes[1])
    assertEquals(Note.E, notes[2])
  }

  @Test
  fun `F Diminished chord should be F G# B`() {
    val chord = Chord(ChordRoot.F, ChordType.DIMINISHED)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.F, notes[0])
    assertEquals(Note.G_SHARP, notes[1])
    assertEquals(Note.B, notes[2])
  }

  @Test
  fun `E Augmented chord should be E G# C`() {
    val chord = Chord(ChordRoot.E, ChordType.AUGMENTED)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.E, notes[0])
    assertEquals(Note.G_SHARP, notes[1])
    assertEquals(Note.C, notes[2])
  }

  @Test
  fun `D Suspended 4th chord should be D G A`() {
    val chord = Chord(ChordRoot.D, ChordType.SUSPENDED_4)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(3, notes.size)
    assertEquals(Note.D, notes[0])
    assertEquals(Note.G, notes[1])
    assertEquals(Note.A, notes[2])
  }

  @Test
  fun `C Ninth chord should have 5 notes`() {
    val chord = Chord(ChordRoot.C, ChordType.NINTH)
    val notes = ChordBuilder.buildChord(chord)

    assertEquals(5, notes.size)
    assertEquals(Note.C, notes[0])
    assertEquals(Note.E, notes[1])
    assertEquals(Note.G, notes[2])
    assertEquals(Note.A_SHARP, notes[3]) // Minor 7th
    assertEquals(Note.D, notes[4]) // Major 9th (14 semitones)
  }

  // NEW TESTS FOR OCTAVES

  @Test
  fun `C9 with octaves should be C4 E4 G4 A#4 D5`() {
    val chord = Chord(ChordRoot.C, ChordType.NINTH)
    val notesWithOctaves = ChordBuilder.buildChordWithOctaves(chord, startOctave = 4)

    assertEquals(5, notesWithOctaves.size)

    // First octave notes
    assertEquals(Note.C, notesWithOctaves[0].note)
    assertEquals(4, notesWithOctaves[0].octave)

    assertEquals(Note.E, notesWithOctaves[1].note)
    assertEquals(4, notesWithOctaves[1].octave)

    assertEquals(Note.G, notesWithOctaves[2].note)
    assertEquals(4, notesWithOctaves[2].octave)

    assertEquals(Note.A_SHARP, notesWithOctaves[3].note)
    assertEquals(4, notesWithOctaves[3].octave)

    // Second octave note (9th is in next octave)
    assertEquals(Note.D, notesWithOctaves[4].note)
    assertEquals(5, notesWithOctaves[4].octave)

    assertEquals("C4 - E4 - G4 - A#4 - D5", ChordBuilder.buildChordStringWithOctaves(chord, 4))
  }

  @Test
  fun `C Major with octaves should all be in same octave`() {
    val chord = Chord(ChordRoot.C, ChordType.MAJOR)
    val notesWithOctaves = ChordBuilder.buildChordWithOctaves(chord, startOctave = 4)

    assertEquals(3, notesWithOctaves.size)

    // All notes in octave 4
    assertEquals(4, notesWithOctaves[0].octave)
    assertEquals(4, notesWithOctaves[1].octave)
    assertEquals(4, notesWithOctaves[2].octave)

    assertEquals("C4 - E4 - G4", ChordBuilder.buildChordStringWithOctaves(chord, 4))
  }

  @Test
  fun `D9 with octaves should span two octaves`() {
    val chord = Chord(ChordRoot.D, ChordType.NINTH)
    val notesWithOctaves = ChordBuilder.buildChordWithOctaves(chord, startOctave = 4)

    assertEquals(5, notesWithOctaves.size)

    // First 3 notes in octave 4
    assertEquals(4, notesWithOctaves[0].octave) // D4
    assertEquals(4, notesWithOctaves[1].octave) // F#4
    assertEquals(4, notesWithOctaves[2].octave) // A4

    // Last 2 notes in octave 5
    assertEquals(5, notesWithOctaves[3].octave) // C5 (10 semitones from D = crosses octave)
    assertEquals(5, notesWithOctaves[4].octave) // E5 (14 semitones from D)
  }

  @Test
  fun `Major chords should be Beginner difficulty`() {
    val chord = Chord(ChordRoot.C, ChordType.MAJOR)
    assertEquals("Beginner", ChordBuilder.getChordDifficulty(chord))
  }

  @Test
  fun `Seventh chords should be Intermediate difficulty`() {
    val chord = Chord(ChordRoot.G, ChordType.SEVENTH)
    assertEquals("Intermediate", ChordBuilder.getChordDifficulty(chord))
  }

  @Test
  fun `Ninth chords should be Advanced difficulty`() {
    val chord = Chord(ChordRoot.C, ChordType.NINTH)
    assertEquals("Advanced", ChordBuilder.getChordDifficulty(chord))
  }

  // OCTAVE COUNT TESTS

  @Test
  fun `C Major should require 1 octave`() {
    val chord = Chord(ChordRoot.C, ChordType.MAJOR)
    assertEquals(1, ChordBuilder.getRequiredOctaveCount(chord))
  }

  @Test
  fun `C Minor should require 1 octave`() {
    val chord = Chord(ChordRoot.C, ChordType.MINOR)
    assertEquals(1, ChordBuilder.getRequiredOctaveCount(chord))
  }

  @Test
  fun `C7 should require 1 octave`() {
    val chord = Chord(ChordRoot.C, ChordType.SEVENTH)
    assertEquals(1, ChordBuilder.getRequiredOctaveCount(chord))
  }

  @Test
  fun `Cmaj7 should require 1 octave`() {
    val chord = Chord(ChordRoot.C, ChordType.MAJOR_SEVENTH)
    assertEquals(1, ChordBuilder.getRequiredOctaveCount(chord))
  }

  @Test
  fun `C9 should require 2 octaves`() {
    val chord = Chord(ChordRoot.C, ChordType.NINTH)
    assertEquals(2, ChordBuilder.getRequiredOctaveCount(chord))
  }

  @Test
  fun `D9 should require 2 octaves`() {
    val chord = Chord(ChordRoot.D, ChordType.NINTH)
    assertEquals(2, ChordBuilder.getRequiredOctaveCount(chord))
  }

  @Test
  fun `All basic chord types should require 1 octave`() {
    val basicTypes =
      listOf(
        ChordType.MAJOR,
        ChordType.MINOR,
        ChordType.DIMINISHED,
        ChordType.AUGMENTED,
        ChordType.SUSPENDED_2,
        ChordType.SUSPENDED_4,
        ChordType.SIXTH,
      )

    basicTypes.forEach { type ->
      val chord = Chord(ChordRoot.C, type)
      assertEquals(
        1,
        ChordBuilder.getRequiredOctaveCount(chord),
        "Expected $type to require 1 octave",
      )
    }
  }
}