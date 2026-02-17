package com.vireal.chordwizard.ui.screens.home.mvi

import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.core.NoteEvent
import com.vireal.chordwizard.midi.core.NoteEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold

data class ActiveMidiNote(
  val deviceId: String,
  val deviceName: String?,
  val channel: Int,
  val note: Int,
  val velocity: Int,
) {
  val noteName: String
    get() = toNoteName(note)

  private fun toNoteName(note: Int): String {
    val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val clamped = note.coerceIn(0, 127)
    val octave = (clamped / 12) - 1
    return "${names[clamped % 12]}$octave"
  }
}

class ObserveActiveMidiNotesUseCase(
  private val midiInputService: MidiInputService,
) {
  fun execute(): Flow<List<ActiveMidiNote>> =
    midiInputService.noteEvents
      .runningFold(linkedMapOf<NoteKey, ActiveMidiNote>()) { acc, event ->
        updateState(acc, event)
      }
      .map { state ->
        state.values
          .sortedWith(compareBy<ActiveMidiNote> { it.deviceName ?: it.deviceId }.thenBy { it.note })
      }

  private fun updateState(
    source: LinkedHashMap<NoteKey, ActiveMidiNote>,
    event: NoteEvent,
  ): LinkedHashMap<NoteKey, ActiveMidiNote> {
    val mutable = LinkedHashMap(source)
    val key = NoteKey(deviceId = event.device.id, channel = event.channel, note = event.note)

    when (event.type) {
      NoteEventType.NOTE_ON -> {
        mutable[key] =
          ActiveMidiNote(
            deviceId = event.device.id,
            deviceName = event.device.name,
            channel = event.channel,
            note = event.note,
            velocity = event.velocity,
          )
      }

      NoteEventType.NOTE_OFF -> mutable.remove(key)
    }

    return mutable
  }

  private data class NoteKey(
    val deviceId: String,
    val channel: Int,
    val note: Int,
  )
}
