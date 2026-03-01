package com.vireal.chordwizard.di

import com.vireal.chordwizard.audio.AudioOutput
import com.vireal.chordwizard.audio.NoOpAudioOutput

actual fun createPlatformAudioOutput(): AudioOutput = NoOpAudioOutput()
