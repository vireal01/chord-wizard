package com.vireal.chordwizard.bluetoothmidi

sealed interface BluetoothMidiError {
  val message: String

  data class PermissionDenied(
    val missing: Set<BluetoothMidiPermission>,
    override val message: String = "Required Bluetooth MIDI permissions were denied.",
  ) : BluetoothMidiError

  data class BluetoothDisabled(
    override val message: String = "Bluetooth is disabled.",
  ) : BluetoothMidiError

  data class ScanFailed(
    override val message: String,
  ) : BluetoothMidiError

  data class ConnectionFailed(
    val deviceId: String?,
    override val message: String,
  ) : BluetoothMidiError

  data class ProtocolError(
    override val message: String,
  ) : BluetoothMidiError

  data class UnsupportedPlatform(
    override val message: String,
  ) : BluetoothMidiError

  data class Unknown(
    override val message: String,
  ) : BluetoothMidiError
}
