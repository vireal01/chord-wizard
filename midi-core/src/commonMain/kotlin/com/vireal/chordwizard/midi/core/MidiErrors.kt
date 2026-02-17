package com.vireal.chordwizard.midi.core

sealed interface MidiError {
  val message: String

  data class ScanFailed(
    override val message: String,
  ) : MidiError

  data class ConnectionFailed(
    val deviceId: String?,
    override val message: String,
  ) : MidiError

  data class ProtocolError(
    override val message: String,
  ) : MidiError

  data class UnsupportedPlatform(
    override val message: String,
  ) : MidiError

  data class Unknown(
    override val message: String,
  ) : MidiError
}
