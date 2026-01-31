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
}
