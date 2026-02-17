package com.vireal.chordwizard.feature.pianorollui

private const val SEMITONES_PER_OCTAVE = 12
private const val DEFAULT_START_NOTE_IN_OCTAVE = 5 // F
private const val DEFAULT_WINDOW_SEMITONES = 24 // F..E (2 octaves)

val DEFAULT_ALLOWED_OCTAVES: IntRange = 0..5
val DEFAULT_NOTE_VISUALIZER_RANGE: IntRange = noteRangeForFOctave(3)

data class PianoViewportState(
  val startOctave: Int,
  val startNoteInOctave: Int,
  val windowSemitones: Int,
  val allowedOctaves: IntRange = DEFAULT_ALLOWED_OCTAVES,
) {
  init {
    require(!allowedOctaves.isEmpty()) { "allowedOctaves cannot be empty" }
    require(startOctave in allowedOctaves) { "startOctave must be within allowedOctaves" }
    require(startNoteInOctave in 0..11) { "startNoteInOctave must be in 0..11" }
    require(windowSemitones > 0) { "windowSemitones must be > 0" }
  }

  val visibleRange: IntRange
    get() {
      val startMidi = midiFor(startOctave, startNoteInOctave)
      return startMidi..(startMidi + windowSemitones - 1)
    }

  val canShiftLeft: Boolean
    get() = startOctave > allowedOctaves.first

  val canShiftRight: Boolean
    get() = startOctave < allowedOctaves.last
}

sealed interface PianoViewportAction {
  data object ShiftLeft : PianoViewportAction

  data object ShiftRight : PianoViewportAction
}

fun pianoViewportStateForRange(
  visibleRange: IntRange,
  allowedOctaves: IntRange = DEFAULT_ALLOWED_OCTAVES,
): PianoViewportState {
  require(!visibleRange.isEmpty()) { "visibleRange cannot be empty" }
  require(!allowedOctaves.isEmpty()) { "allowedOctaves cannot be empty" }
  val first = visibleRange.first
  val startOctave = octaveForMidi(first).coerceIn(allowedOctaves.first, allowedOctaves.last)
  val startNoteInOctave = ((first % SEMITONES_PER_OCTAVE) + SEMITONES_PER_OCTAVE) % SEMITONES_PER_OCTAVE
  val windowSemitones = visibleRange.last - visibleRange.first + 1
  return PianoViewportState(
    startOctave = startOctave,
    startNoteInOctave = startNoteInOctave,
    windowSemitones = windowSemitones,
    allowedOctaves = allowedOctaves,
  )
}

fun reducePianoViewportState(
  state: PianoViewportState,
  action: PianoViewportAction,
): PianoViewportState =
  when (action) {
    PianoViewportAction.ShiftLeft -> shiftOctaves(state, -1)
    PianoViewportAction.ShiftRight -> shiftOctaves(state, 1)
  }

fun noteRangeForFOctave(
  octave: Int,
  windowSemitones: Int = DEFAULT_WINDOW_SEMITONES,
): IntRange {
  val startMidi = midiFor(octave, DEFAULT_START_NOTE_IN_OCTAVE)
  return startMidi..(startMidi + windowSemitones - 1)
}

private fun shiftOctaves(
  state: PianoViewportState,
  delta: Int,
): PianoViewportState {
  val shiftedOctave = (state.startOctave + delta).coerceIn(state.allowedOctaves.first, state.allowedOctaves.last)
  return state.copy(startOctave = shiftedOctave)
}

private fun midiFor(
  octave: Int,
  noteInOctave: Int,
): Int = SEMITONES_PER_OCTAVE * (octave + 1) + noteInOctave

private fun octaveForMidi(midiNote: Int): Int = (midiNote / SEMITONES_PER_OCTAVE) - 1
