package com.vireal.chordwizard.audio

import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.core.NoteEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MidiAudioBridge(
  private val midiInputService: MidiInputService,
  private val instrumentEngine: InstrumentEngine,
  private val playbackPolicy: AudioPlaybackPolicy,
) {
  private val mutex = Mutex()
  private val attachedRoutes = linkedSetOf<AudioRouteKey>()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var midiJob: Job? = null

  suspend fun attach(route: AudioRouteKey) {
    mutex.withLock {
      attachedRoutes += route
      reevaluatePlaybackLocked()
    }
  }

  suspend fun detach(route: AudioRouteKey) {
    mutex.withLock {
      attachedRoutes -= route
      reevaluatePlaybackLocked()
    }
  }

  suspend fun detachAll() {
    mutex.withLock {
      attachedRoutes.clear()
      reevaluatePlaybackLocked()
    }
  }

  fun dispose() {
    scope.cancel()
  }

  private suspend fun reevaluatePlaybackLocked() {
    val shouldPlay = attachedRoutes.any(playbackPolicy::isPlaybackEnabled)
    val isPlaying = midiJob != null

    if (shouldPlay && !isPlaying) {
      instrumentEngine.start()
      midiJob =
        scope.launch {
          midiInputService.noteEvents.collect { event ->
            when (event.type) {
              NoteEventType.NOTE_ON -> {
                if (event.velocity > 0) {
                  instrumentEngine.noteOn(
                    note = event.note,
                    velocity = event.velocity,
                    channel = event.channel,
                  )
                } else {
                  instrumentEngine.noteOff(
                    note = event.note,
                    channel = event.channel,
                  )
                }
              }

              NoteEventType.NOTE_OFF -> {
                instrumentEngine.noteOff(
                  note = event.note,
                  channel = event.channel,
                )
              }
            }
          }
        }
      return
    }

    if (!shouldPlay && isPlaying) {
      midiJob?.cancel()
      midiJob = null
      instrumentEngine.allNotesOff()
      instrumentEngine.stop()
    }
  }
}
