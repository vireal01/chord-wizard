package com.vireal.chordwizard.domain.model

/**
 * Chord root note (C, D, E, F, G, A, B)
 */
enum class ChordRoot(
  val displayName: String,
) {
  C("C"),
  D("D"),
  E("E"),
  F("F"),
  G("G"),
  A("A"),
  B("B"),
}

/**
 * Chord type/variation
 */
enum class ChordType(
  val displayName: String,
  val symbol: String,
) {
  MAJOR("Major", ""),
  MINOR("Minor", "m"),
  SEVENTH("Seventh", "7"),
  MAJOR_SEVENTH("Major 7th", "maj7"),
  MINOR_SEVENTH("Minor 7th", "m7"),
  DIMINISHED("Diminished", "dim"),
  AUGMENTED("Augmented", "aug"),
  SUSPENDED_2("Suspended 2nd", "sus2"),
  SUSPENDED_4("Suspended 4th", "sus4"),
  SIXTH("Sixth", "6"),
  NINTH("Ninth", "9"),
}

/**
 * Complete chord with root and type
 */
data class Chord(
  val root: ChordRoot,
  val type: ChordType = ChordType.MAJOR,
) {
  val displayName: String
    get() = "${root.displayName}${type.symbol}"

  val fullName: String
    get() = "${root.displayName} ${type.displayName}"
}