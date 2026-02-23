package com.vireal.chordwizard.ui.screens.chordtrainer.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrainerDeckGeneratorTest {
  @Test
  fun `buildPool creates cartesian product of selected roots and families`() {
    val config =
      TrainerSessionConfig(
        selectedFamilies = setOf(TrainerChordFamily.MAJOR, TrainerChordFamily.MINOR),
        selectedRoots = setOf(TrainerRoot.C, TrainerRoot.D),
        cardCount = 10,
      )

    val pool = TrainerDeckGenerator.buildPool(config)

    assertEquals(4, pool.size)
    assertTrue(pool.contains(TrainerCard(TrainerRoot.C, TrainerChordFamily.MAJOR)))
    assertTrue(pool.contains(TrainerCard(TrainerRoot.C, TrainerChordFamily.MINOR)))
    assertTrue(pool.contains(TrainerCard(TrainerRoot.D, TrainerChordFamily.MAJOR)))
    assertTrue(pool.contains(TrainerCard(TrainerRoot.D, TrainerChordFamily.MINOR)))
  }

  @Test
  fun `buildDeck caps size when card count exceeds pool size`() {
    val config =
      TrainerSessionConfig(
        selectedFamilies = setOf(TrainerChordFamily.MAJOR),
        selectedRoots = setOf(TrainerRoot.C, TrainerRoot.D),
        cardCount = 10,
      )

    val deck = TrainerDeckGenerator.buildDeck(config, random = Random(42))

    assertEquals(2, deck.size)
    assertEquals(deck.size, deck.toSet().size)
  }
}
