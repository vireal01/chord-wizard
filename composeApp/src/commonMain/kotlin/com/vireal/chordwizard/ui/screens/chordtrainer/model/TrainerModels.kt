package com.vireal.chordwizard.ui.screens.chordtrainer.model

import kotlin.math.min
import kotlin.random.Random

enum class TrainerRoot(
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
}

enum class TrainerChordFamily(
  val displayName: String,
  val symbol: String,
) {
  MAJOR("Major", ""),
  MINOR("Minor", "m"),
  SEVENTH("Seventh", "7"),
  FIFTH("Fifth", "5"),
}

enum class TrainerMode {
  Guided,
  Flashcards,
}

enum class TrainerAdvanceMode {
  Manual,
  Auto,
}

data class TrainerCard(
  val root: TrainerRoot,
  val family: TrainerChordFamily,
) {
  private val intervals: List<Int>
    get() =
      when (family) {
        TrainerChordFamily.MAJOR -> listOf(0, 4, 7)
        TrainerChordFamily.MINOR -> listOf(0, 3, 7)
        TrainerChordFamily.SEVENTH -> listOf(0, 4, 7, 10)
        TrainerChordFamily.FIFTH -> listOf(0, 7)
      }

  val displayName: String
    get() = "${root.displayName}${family.symbol}"

  val fullName: String
    get() = "${root.displayName} ${family.displayName}"

  val targetPitchClasses: Set<Int>
    get() = intervals.map { normalizePitchClass(root.chromaticPosition + it) }.toSet()

  val targetNoteNames: List<String>
    get() = intervals.map { pitchClassName(normalizePitchClass(root.chromaticPosition + it)) }

  fun toTargetMidiSequence(
    baseOctave: Int = 4,
  ): List<Int> {
    val rootMidi = ((baseOctave + 1) * 12) + root.chromaticPosition
    return intervals.map { rootMidi + it }
  }
}

data class TrainerSessionConfig(
  val selectedFamilies: Set<TrainerChordFamily>,
  val selectedRoots: Set<TrainerRoot>,
  val mode: TrainerMode = TrainerMode.Guided,
  val advanceMode: TrainerAdvanceMode = TrainerAdvanceMode.Manual,
  val cardCount: Int = DEFAULT_CARD_COUNT,
) {
  val canStart: Boolean
    get() = selectedFamilies.isNotEmpty() && selectedRoots.isNotEmpty()

  val poolSize: Int
    get() = selectedFamilies.size * selectedRoots.size

  val effectiveDeckSize: Int
    get() = min(cardCount.coerceAtLeast(1), poolSize)

  companion object {
    const val DEFAULT_CARD_COUNT = 10
  }
}

object TrainerDeckGenerator {
  fun buildPool(config: TrainerSessionConfig): List<TrainerCard> {
    if (!config.canStart) return emptyList()

    val orderedRoots = TrainerRoot.entries.filter { it in config.selectedRoots }
    val orderedFamilies = TrainerChordFamily.entries.filter { it in config.selectedFamilies }

    return buildList {
      orderedRoots.forEach { root ->
        orderedFamilies.forEach { family ->
          add(TrainerCard(root = root, family = family))
        }
      }
    }
  }

  fun buildDeck(
    config: TrainerSessionConfig,
    random: Random = Random.Default,
  ): List<TrainerCard> {
    val pool = buildPool(config)
    if (pool.isEmpty()) return emptyList()

    val targetSize = min(config.cardCount.coerceAtLeast(1), pool.size)
    return pool.shuffled(random).take(targetSize)
  }
}

fun normalizePitchClass(value: Int): Int = ((value % 12) + 12) % 12

fun pitchClassName(pitchClass: Int): String =
  when (normalizePitchClass(pitchClass)) {
    0 -> "C"
    1 -> "C#"
    2 -> "D"
    3 -> "D#"
    4 -> "E"
    5 -> "F"
    6 -> "F#"
    7 -> "G"
    8 -> "G#"
    9 -> "A"
    10 -> "A#"
    else -> "B"
  }
