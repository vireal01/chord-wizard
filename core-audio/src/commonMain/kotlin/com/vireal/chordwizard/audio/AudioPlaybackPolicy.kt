package com.vireal.chordwizard.audio

enum class AudioRouteKey {
  HOME,
  CHORD_LIBRARY,
  CHORD_TRAINER_SETUP,
  CHORD_TRAINER_SESSION,
  CHORD_DETAILS,
  SETTINGS,
  NOTE_VISUALIZER,
}

class AudioPlaybackPolicy(
  val enabledRoutes: Set<AudioRouteKey>,
) {
  fun isPlaybackEnabled(route: AudioRouteKey): Boolean = route in enabledRoutes
}
