package com.vireal.chordwizard.di

import com.vireal.chordwizard.audio.AudioOutput

expect fun createPlatformAudioOutput(): AudioOutput
