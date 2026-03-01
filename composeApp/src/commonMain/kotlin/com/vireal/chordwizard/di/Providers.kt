package com.vireal.chordwizard.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.vireal.chordwizard.Greeting
import com.vireal.chordwizard.audio.AudioPlaybackPolicy
import com.vireal.chordwizard.audio.AudioRouteKey
import com.vireal.chordwizard.audio.InstrumentEngine
import com.vireal.chordwizard.audio.InstrumentEngineFacade
import com.vireal.chordwizard.audio.InstrumentBackend
import com.vireal.chordwizard.audio.MidiAudioBridge
import com.vireal.chordwizard.audio.SamplerEngine
import com.vireal.chordwizard.audio.SynthEngine
import com.vireal.chordwizard.audio.SynthEngineImpl
import com.vireal.chordwizard.audio.StubSamplerEngine
import com.vireal.chordwizard.bluetoothmidi.BluetoothMidiService
import com.vireal.chordwizard.bluetoothmidi.createBluetoothMidiService
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.midi.usb.createUsbMidiInputService
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.TrainerSetupMemoryRepository
import dev.zacsweers.metro.Provides

/**
 * Provides basic application dependencies
 */
interface AppProvides {
  @Provides
  fun provideGreeting(): Greeting = Greeting()

  @Provides
  fun provideStoreFactory(): StoreFactory = LoggingStoreFactory(DefaultStoreFactory())

  @Provides
  fun provideBluetoothMidiService(): BluetoothMidiService = SharedServices.bluetoothMidiService

  @Provides
  fun provideMidiInputService(): MidiInputService = SharedServices.usbMidiInputService

  @Provides
  fun provideInstrumentEngine(): InstrumentEngine = SharedServices.instrumentEngine

  @Provides
  fun provideSynthEngine(): SynthEngine = SharedServices.synthEngine

  @Provides
  fun provideSamplerEngine(): SamplerEngine = SharedServices.samplerEngine

  @Provides
  fun provideAudioPlaybackPolicy(): AudioPlaybackPolicy = SharedServices.audioPlaybackPolicy

  @Provides
  fun provideMidiAudioBridge(): MidiAudioBridge = SharedServices.midiAudioBridge

  @Provides
  fun provideTrainerSetupMemoryRepository(): TrainerSetupMemoryRepository = SharedServices.trainerSetupMemoryRepository
}

private object SharedServices {
  val bluetoothMidiService: BluetoothMidiService by lazy { createBluetoothMidiService() }
  val usbMidiInputService: MidiInputService by lazy { createUsbMidiInputService() }
  val synthEngine: SynthEngine by lazy { SynthEngineImpl(audioOutput = createPlatformAudioOutput()) }
  val samplerEngine: SamplerEngine by lazy { StubSamplerEngine() }
  val instrumentEngine: InstrumentEngineFacade by lazy {
    InstrumentEngineFacade(
      synthEngine = synthEngine,
      samplerEngine = samplerEngine,
      initialBackend = InstrumentBackend.SYNTH,
    )
  }
  val audioPlaybackPolicy: AudioPlaybackPolicy by lazy {
    AudioPlaybackPolicy(enabledRoutes = setOf(AudioRouteKey.NOTE_VISUALIZER))
  }
  val midiAudioBridge: MidiAudioBridge by lazy {
    MidiAudioBridge(
      midiInputService = usbMidiInputService,
      instrumentEngine = instrumentEngine,
      playbackPolicy = audioPlaybackPolicy,
    )
  }
  val trainerSetupMemoryRepository: TrainerSetupMemoryRepository by lazy { TrainerSetupMemoryRepository() }
}
