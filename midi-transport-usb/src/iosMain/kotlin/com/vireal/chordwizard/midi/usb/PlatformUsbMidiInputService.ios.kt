package com.vireal.chordwizard.midi.usb

actual class PlatformUsbMidiInputService actual constructor() :
  StubUsbMidiInputService(platform = UsbMidiPlatform.IOS)
