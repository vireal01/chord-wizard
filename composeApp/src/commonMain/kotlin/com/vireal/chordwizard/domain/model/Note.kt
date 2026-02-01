package com.vireal.chordwizard.domain.model

/**
 * Musical note with chromatic position
 */
enum class Note(
  val displayName: String,
  val chromaticPosition: Int,
) {
  C("C", 0),
  C_SHARP("C#", 1),
  D("D", 2),
  D_SHARP("D#", 3),
  E("E", 4),
  F("F", 5),
  F_SHARP("F#", 6),
  G("G", 7),
  G_SHARP("G#", 8),
  A("A", 9),
  A_SHARP("A#", 10),
  B("B", 11),
  ;

  companion object {
    /**
     * Get note by chromatic position (0-11)
     */
    fun fromPosition(position: Int): Note {
      val normalizedPosition = position % 12
      return entries.first { it.chromaticPosition == normalizedPosition }
    }

    /**
     * Get note by ChordRoot
     */
    fun fromChordRoot(root: ChordRoot): Note =
      when (root) {
        ChordRoot.C -> C
        ChordRoot.D -> D
        ChordRoot.E -> E
        ChordRoot.F -> F
        ChordRoot.G -> G
        ChordRoot.A -> A
        ChordRoot.B -> B
      }
  }

  /**
   * Add interval (semitones) to this note
   */
  operator fun plus(semitones: Int): Note {
    val newPosition = (chromaticPosition + semitones) % 12
    return fromPosition(newPosition)
  }
}

/**
 * Note with octave information
 */
data class NoteWithOctave(
  val note: Note,
  val octave: Int,
) {
  val displayName: String
    get() = "${note.displayName}$octave"

  /**
   * Get absolute chromatic position (C0 = 0, C1 = 12, C2 = 24, etc.)
   */
  val absolutePosition: Int
    get() = octave * 12 + note.chromaticPosition

  companion object {
    /**
     * Create from absolute chromatic position
     */
    fun fromAbsolutePosition(position: Int): NoteWithOctave {
      val octave = position / 12
      val notePosition = position % 12
      return NoteWithOctave(Note.fromPosition(notePosition), octave)
    }
  }

  /**
   * Add interval (semitones) to this note with octave
   */
  operator fun plus(semitones: Int): NoteWithOctave = fromAbsolutePosition(absolutePosition + semitones)
}