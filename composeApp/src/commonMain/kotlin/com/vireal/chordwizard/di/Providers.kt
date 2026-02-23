package com.vireal.chordwizard.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.vireal.chordwizard.Greeting
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
  fun provideTrainerSetupMemoryRepository(): TrainerSetupMemoryRepository = SharedServices.trainerSetupMemoryRepository
}

private object SharedServices {
  val bluetoothMidiService: BluetoothMidiService by lazy { createBluetoothMidiService() }
  val usbMidiInputService: MidiInputService by lazy { createUsbMidiInputService() }
  val trainerSetupMemoryRepository: TrainerSetupMemoryRepository by lazy { TrainerSetupMemoryRepository() }
}
