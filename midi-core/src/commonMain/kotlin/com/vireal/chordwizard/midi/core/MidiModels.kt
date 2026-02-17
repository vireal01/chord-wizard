package com.vireal.chordwizard.midi.core

data class MidiDevice(
  val id: String,
  val name: String?,
  val transport: MidiTransport,
  val rssi: Int? = null,
  val isConnectable: Boolean = true,
  val manufacturer: String? = null,
  val product: String? = null,
  val lastSeenEpochMillis: Long? = null,
)

enum class MidiTransport {
  USB,
  BLUETOOTH,
  UNKNOWN,
}

data class MidiDeviceRef(
  val id: String,
  val name: String?,
)

data class MidiPacket(
  val device: MidiDeviceRef,
  val bytes: ByteArray,
  val receivedAtEpochMillis: Long,
)

data class MidiMessageEvent(
  val device: MidiDeviceRef,
  val message: MidiMessage,
  val receivedAtEpochMillis: Long,
)

enum class NoteEventType {
  NOTE_ON,
  NOTE_OFF,
}

data class NoteEvent(
  val device: MidiDeviceRef,
  val channel: Int,
  val note: Int,
  val velocity: Int,
  val type: NoteEventType,
  val receivedAtEpochMillis: Long,
)

sealed interface MidiMessage {
  val channel: Int?

  data class NoteOff(
    override val channel: Int,
    val note: Int,
    val velocity: Int,
  ) : MidiMessage

  data class NoteOn(
    override val channel: Int,
    val note: Int,
    val velocity: Int,
  ) : MidiMessage

  data class PolyphonicKeyPressure(
    override val channel: Int,
    val note: Int,
    val pressure: Int,
  ) : MidiMessage

  data class ControlChange(
    override val channel: Int,
    val controller: Int,
    val value: Int,
  ) : MidiMessage

  data class ProgramChange(
    override val channel: Int,
    val program: Int,
  ) : MidiMessage

  data class ChannelPressure(
    override val channel: Int,
    val pressure: Int,
  ) : MidiMessage

  data class PitchBend(
    override val channel: Int,
    val value14Bit: Int,
  ) : MidiMessage

  data class System(
    val status: Int,
    val data: ByteArray,
  ) : MidiMessage {
    override val channel: Int? = null
  }

  data class Raw(
    val bytes: ByteArray,
  ) : MidiMessage {
    override val channel: Int? = null
  }
}
