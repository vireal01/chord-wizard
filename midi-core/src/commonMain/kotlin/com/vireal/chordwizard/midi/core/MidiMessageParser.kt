package com.vireal.chordwizard.midi.core

object MidiMessageParser {
  fun parse(packet: MidiPacket): List<MidiMessageEvent> {
    val messages = parse(packet.bytes)
    return messages.map { message ->
      MidiMessageEvent(
        device = packet.device,
        message = message,
        receivedAtEpochMillis = packet.receivedAtEpochMillis,
      )
    }
  }

  fun parse(bytes: ByteArray): List<MidiMessage> {
    if (bytes.isEmpty()) {
      return emptyList()
    }

    val result = mutableListOf<MidiMessage>()
    var index = 0
    var runningStatus: Int? = null

    while (index < bytes.size) {
      val current = bytes[index].toInt() and 0xFF
      val status: Int

      if (current and 0x80 != 0) {
        status = current
        index += 1

        if (status < 0xF0) {
          runningStatus = status
        } else if (status >= 0xF8) {
          result += MidiMessage.System(status = status, data = byteArrayOf())
          continue
        } else {
          runningStatus = null
        }
      } else {
        val rs = runningStatus
        if (rs == null) {
          result += MidiMessage.Raw(bytes.copyOfRange(index, bytes.size))
          break
        }
        status = rs
      }

      when (status and 0xF0) {
        0x80, 0x90, 0xA0, 0xB0, 0xE0 -> {
          if (index + 1 >= bytes.size) {
            result += MidiMessage.Raw(withStatus(status, bytes, index))
            break
          }

          val data1 = bytes[index].toInt() and 0x7F
          val data2 = bytes[index + 1].toInt() and 0x7F
          index += 2

          result +=
            when (status and 0xF0) {
              0x80 -> MidiMessage.NoteOff(channel = status and 0x0F, note = data1, velocity = data2)
              0x90 -> MidiMessage.NoteOn(channel = status and 0x0F, note = data1, velocity = data2)
              0xA0 -> MidiMessage.PolyphonicKeyPressure(channel = status and 0x0F, note = data1, pressure = data2)
              0xB0 -> MidiMessage.ControlChange(channel = status and 0x0F, controller = data1, value = data2)
              else -> MidiMessage.PitchBend(channel = status and 0x0F, value14Bit = (data2 shl 7) or data1)
            }
        }

        0xC0, 0xD0 -> {
          if (index >= bytes.size) {
            result += MidiMessage.Raw(withStatus(status, bytes, index))
            break
          }

          val data1 = bytes[index].toInt() and 0x7F
          index += 1

          result +=
            if ((status and 0xF0) == 0xC0) {
              MidiMessage.ProgramChange(channel = status and 0x0F, program = data1)
            } else {
              MidiMessage.ChannelPressure(channel = status and 0x0F, pressure = data1)
            }
        }

        else -> {
          val system = parseSystem(status = status, bytes = bytes, index = index)
          result += system.message
          index = system.nextIndex
        }
      }
    }

    return result
  }

  fun toNoteEvents(event: MidiMessageEvent): List<NoteEvent> =
    when (val message = event.message) {
      is MidiMessage.NoteOff -> {
        listOf(
          NoteEvent(
            device = event.device,
            channel = message.channel,
            note = message.note,
            velocity = message.velocity,
            type = NoteEventType.NOTE_OFF,
            receivedAtEpochMillis = event.receivedAtEpochMillis,
          ),
        )
      }

      is MidiMessage.NoteOn -> {
        val type = if (message.velocity == 0) NoteEventType.NOTE_OFF else NoteEventType.NOTE_ON
        listOf(
          NoteEvent(
            device = event.device,
            channel = message.channel,
            note = message.note,
            velocity = message.velocity,
            type = type,
            receivedAtEpochMillis = event.receivedAtEpochMillis,
          ),
        )
      }

      else -> emptyList()
    }

  private data class SystemParseResult(
    val message: MidiMessage,
    val nextIndex: Int,
  )

  private fun parseSystem(status: Int, bytes: ByteArray, index: Int): SystemParseResult {
    val remaining = bytes.size - index

    return when (status) {
      0xF0 -> {
        val end = bytes.indexOfFirstFrom(index) { (it.toInt() and 0xFF) == 0xF7 }
        if (end == -1) {
          SystemParseResult(
            message = MidiMessage.System(status = status, data = bytes.copyOfRange(index, bytes.size)),
            nextIndex = bytes.size,
          )
        } else {
          SystemParseResult(
            message = MidiMessage.System(status = status, data = bytes.copyOfRange(index, end + 1)),
            nextIndex = end + 1,
          )
        }
      }

      0xF1, 0xF3 -> {
        if (remaining < 1) {
          SystemParseResult(MidiMessage.Raw(byteArrayOf(status.toByte())), bytes.size)
        } else {
          SystemParseResult(
            message = MidiMessage.System(status = status, data = byteArrayOf(bytes[index])),
            nextIndex = index + 1,
          )
        }
      }

      0xF2 -> {
        if (remaining < 2) {
          SystemParseResult(MidiMessage.Raw(withStatus(status, bytes, index)), bytes.size)
        } else {
          SystemParseResult(
            message = MidiMessage.System(status = status, data = bytes.copyOfRange(index, index + 2)),
            nextIndex = index + 2,
          )
        }
      }

      else -> {
        SystemParseResult(
          message = MidiMessage.System(status = status, data = byteArrayOf()),
          nextIndex = index,
        )
      }
    }
  }

  private fun withStatus(status: Int, bytes: ByteArray, index: Int): ByteArray {
    val tail = if (index >= bytes.size) byteArrayOf() else bytes.copyOfRange(index, bytes.size)
    return byteArrayOf(status.toByte()) + tail
  }

  private inline fun ByteArray.indexOfFirstFrom(
    startIndex: Int,
    predicate: (Byte) -> Boolean,
  ): Int {
    for (i in startIndex until size) {
      if (predicate(this[i])) {
        return i
      }
    }
    return -1
  }
}
