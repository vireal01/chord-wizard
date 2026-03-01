package com.vireal.chordwizard.di

import com.vireal.chordwizard.audio.InstrumentEngine
import com.vireal.chordwizard.audio.MidiAudioBridge
import com.vireal.chordwizard.audio.SynthEngine
import com.vireal.chordwizard.bluetoothmidi.BluetoothMidiService
import com.vireal.chordwizard.midi.core.MidiInputService
import com.vireal.chordwizard.ui.screens.chorddetails.mvi.ChordDetailsStoreProvider
import com.vireal.chordwizard.ui.screens.chordlibrary.mvi.ChordLibraryStoreProvider
import com.vireal.chordwizard.ui.screens.chordtrainer.session.mvi.ChordTrainerSessionStoreProvider
import com.vireal.chordwizard.ui.screens.chordtrainer.setup.mvi.ChordTrainerSetupStoreProvider
import com.vireal.chordwizard.ui.screens.home.mvi.HomeStoreProvider
import com.vireal.chordwizard.ui.screens.settings.mvi.SettingsStoreProvider
import dev.zacsweers.metro.DependencyGraph

/**
 * Main application DI component
 * Manages all application-level dependencies
 */
@DependencyGraph
abstract class AppComponent : AppProvides {
  /**
   * Provides Repository instance
   */
  abstract val repository: AppRepository

  /**
   * Provides Bluetooth MIDI service.
   */
  abstract val bluetoothMidiService: BluetoothMidiService

  /**
   * Provides selected MIDI input service (USB by default).
   */
  abstract val midiInputService: MidiInputService

  /**
   * Provides currently selected instrument engine.
   */
  abstract val instrumentEngine: InstrumentEngine

  /**
   * Provides synth engine implementation.
   */
  abstract val synthEngine: SynthEngine

  /**
   * Provides MIDI-to-audio bridge for screen-scoped playback.
   */
  abstract val midiAudioBridge: MidiAudioBridge

  /**
   * Provides HomeStoreProvider for MVI
   */
  abstract val homeStoreProvider: HomeStoreProvider

  /**
   * Provides ChordLibraryStoreProvider for MVI
   */
  abstract val chordLibraryStoreProvider: ChordLibraryStoreProvider

  /**
   * Provides ChordDetailsStoreProvider for MVI
   */
  abstract val chordDetailsStoreProvider: ChordDetailsStoreProvider

  /**
   * Provides SettingsStoreProvider for MVI
   */
  abstract val settingsStoreProvider: SettingsStoreProvider

  /**
   * Provides ChordTrainerSetupStoreProvider for MVI
   */
  abstract val chordTrainerSetupStoreProvider: ChordTrainerSetupStoreProvider

  /**
   * Provides ChordTrainerSessionStoreProvider for MVI
   */
  abstract val chordTrainerSessionStoreProvider: ChordTrainerSessionStoreProvider
}
