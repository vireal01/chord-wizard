package com.vireal.chordwizard.bluetoothmidi

actual class PlatformBluetoothMidiService actual constructor() :
  StubBluetoothMidiService(platform = BluetoothMidiPlatform.WEB_JS)
