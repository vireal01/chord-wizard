package com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerAdvanceMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerChordFamily
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerRoot
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChordTrainerSessionStoreTest {
  @Test
  fun `manual mode advances only after correct chord`() {
    val store =
      ChordTrainerSessionStoreFactory(
        storeFactory = DefaultStoreFactory(),
        config = baseConfig(advanceMode = TrainerAdvanceMode.Manual),
      ).create()

    store.accept(ChordTrainerSessionStore.Intent.NextChord)
    assertEquals(0, store.state.currentIndex)
    assertEquals(0, store.state.solvedCount)

    store.accept(ChordTrainerSessionStore.Intent.UpdateMidiNotes(setOf(72, 76, 79)))
    assertTrue(store.state.isCurrentCorrect)

    store.accept(ChordTrainerSessionStore.Intent.NextChord)
    assertTrue(store.state.isFinished)
    assertEquals(1, store.state.solvedCount)
  }

  @Test
  fun `skip moves to next card and increments skipped counter`() {
    val store =
      ChordTrainerSessionStoreFactory(
        storeFactory = DefaultStoreFactory(),
        config = baseConfig(advanceMode = TrainerAdvanceMode.Manual),
      ).create()

    store.accept(ChordTrainerSessionStore.Intent.SkipChord)

    assertTrue(store.state.isFinished)
    assertEquals(1, store.state.skippedCount)
    assertEquals(0, store.state.solvedCount)
  }

  @Test
  fun `mistake is counted when chord had incorrect attempt before success`() {
    val store =
      ChordTrainerSessionStoreFactory(
        storeFactory = DefaultStoreFactory(),
        config = baseConfig(advanceMode = TrainerAdvanceMode.Manual),
      ).create()

    store.accept(ChordTrainerSessionStore.Intent.UpdateMidiNotes(setOf(61)))
    assertFalse(store.state.isCurrentCorrect)
    assertTrue(store.state.currentCardHadMistake)

    store.accept(ChordTrainerSessionStore.Intent.UpdateMidiNotes(setOf(60, 64, 67)))
    assertTrue(store.state.isCurrentCorrect)
    store.accept(ChordTrainerSessionStore.Intent.NextChord)

    assertEquals(1, store.state.solvedCount)
    assertEquals(1, store.state.mistakesCount)
  }

  @Test
  fun `auto mode exposes auto-advance signal when chord is correct`() {
    val store =
      ChordTrainerSessionStoreFactory(
        storeFactory = DefaultStoreFactory(),
        config = baseConfig(advanceMode = TrainerAdvanceMode.Auto),
      ).create()

    store.accept(ChordTrainerSessionStore.Intent.UpdateMidiNotes(setOf(72, 76, 79)))

    assertTrue(store.state.isCurrentCorrect)
    assertTrue(store.state.shouldAutoAdvance)
  }

  private fun baseConfig(advanceMode: TrainerAdvanceMode): TrainerSessionConfig =
    TrainerSessionConfig(
      selectedFamilies = setOf(TrainerChordFamily.MAJOR),
      selectedRoots = setOf(TrainerRoot.C),
      advanceMode = advanceMode,
      cardCount = 1,
    )
}
