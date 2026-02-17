package com.vireal.chordwizard.feature.pianorollui

import com.vireal.chordwizard.midi.core.NoteEvent
import com.vireal.chordwizard.midi.core.NoteEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold

internal fun Flow<NoteEvent>.trackPressedKeys(): Flow<List<PressedKeyUi>> =
  runningFold(PressedKeysAccumulator()) { acc, event ->
    acc.apply(event)
  }.map { acc ->
    acc.snapshot()
  }

private class PressedKeysAccumulator {
  private val bySource = linkedMapOf<LiveNoteKey, PressedKeyUi>()

  fun apply(event: NoteEvent): PressedKeysAccumulator {
    val key = LiveNoteKey(event.device.id, event.channel, event.note)
    val isNoteOff = event.type == NoteEventType.NOTE_OFF || (event.type == NoteEventType.NOTE_ON && event.velocity == 0)

    if (isNoteOff) {
      bySource.remove(key)
    } else {
      bySource[key] =
        PressedKeyUi(
          note = event.note,
          velocity = event.velocity,
          isTarget = false,
          isCorrect = false,
          startedAt = event.receivedAtEpochMillis,
        )
    }

    return this
  }

  fun snapshot(): List<PressedKeyUi> {
    if (bySource.isEmpty()) return emptyList()

    val mergedByNote = linkedMapOf<Int, PressedKeyUi>()
    bySource.values.forEach { value ->
      val existing = mergedByNote[value.note]
      if (existing == null) {
        mergedByNote[value.note] = value
      } else {
        mergedByNote[value.note] =
          PressedKeyUi(
            note = value.note,
            velocity = maxOf(existing.velocity, value.velocity),
            isTarget = false,
            isCorrect = false,
            startedAt = minOf(existing.startedAt, value.startedAt),
          )
      }
    }

    return mergedByNote.values.sortedBy { it.note }
  }
}

private data class LiveNoteKey(
  val deviceId: String,
  val channel: Int,
  val note: Int,
)
