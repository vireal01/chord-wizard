package com.vireal.chordwizard.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class SynthEngineImpl(
  private val audioOutput: AudioOutput = NoOpAudioOutput(),
  private val config: SynthEngineConfig = SynthEngineConfig(),
) : SynthEngine {
  private val _state = MutableStateFlow(InstrumentEngineState(InstrumentEngineState.Status.STOPPED))
  override val state: StateFlow<InstrumentEngineState> = _state

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val lifecycleMutex = Mutex()
  private val voiceMutex = Mutex()

  private var waveform: SynthWaveform = SynthWaveform.SINE
  private var masterVolume: Float = config.initialMasterVolume.coerceIn(0f, 1f)
  private var renderJob: Job? = null
  private var noteSequence: Long = 0L

  private val voices = mutableListOf<Voice>()

  override suspend fun start() {
    lifecycleMutex.withLock {
      if (renderJob != null) return

      _state.value = InstrumentEngineState(InstrumentEngineState.Status.STARTING)
      try {
        audioOutput.start(sampleRateHz = config.sampleRateHz, channels = 1)
      } catch (t: Throwable) {
        _state.value = InstrumentEngineState(InstrumentEngineState.Status.FAILED, t.message ?: "Audio output start failed")
        return
      }

      val startedJob = scope.launch { runRenderLoop() }
      renderJob = startedJob

      _state.value = InstrumentEngineState(InstrumentEngineState.Status.RUNNING)
    }
  }

  override suspend fun stop() {
    lifecycleMutex.withLock {
      val job = renderJob
      renderJob = null
      job?.cancelAndJoin()

      voiceMutex.withLock {
        voices.clear()
      }
      audioOutput.stop()
      _state.value = InstrumentEngineState(InstrumentEngineState.Status.STOPPED)
    }
  }

  override suspend fun noteOn(
    note: Int,
    velocity: Int,
    channel: Int,
  ) {
    if (_state.value.status != InstrumentEngineState.Status.RUNNING) return
    if (note !in MIDI_NOTE_RANGE || velocity !in MIDI_VALUE_RANGE || channel !in MIDI_CHANNEL_RANGE) return
    if (velocity == 0) {
      noteOff(note = note, channel = channel)
      return
    }

    val voiceAmplitude = (velocity / MIDI_MAX_FLOAT) * masterVolume
    val frequency = midiNoteToFrequency(note)

    voiceMutex.withLock {
      voices.removeAll { it.note == note && it.channel == channel }

      if (voices.size >= config.maxVoices) {
        val oldest = voices.minByOrNull { it.sequence }
        if (oldest != null) voices.remove(oldest)
      }

      noteSequence += 1L
      voices +=
        Voice(
          note = note,
          channel = channel,
          frequencyHz = frequency,
          targetAmplitude = voiceAmplitude,
          currentAmplitude = 0f,
          phase = 0f,
          sequence = noteSequence,
          released = false,
        )
    }
  }

  override suspend fun noteOff(
    note: Int,
    channel: Int,
  ) {
    if (_state.value.status != InstrumentEngineState.Status.RUNNING) return
    if (note !in MIDI_NOTE_RANGE || channel !in MIDI_CHANNEL_RANGE) return

    voiceMutex.withLock {
      voices.forEach { voice ->
        if (voice.note == note && voice.channel == channel) {
          voice.released = true
        }
      }
    }
  }

  override suspend fun allNotesOff() {
    if (_state.value.status != InstrumentEngineState.Status.RUNNING) return

    voiceMutex.withLock {
      voices.forEach { it.released = true }
    }
  }

  override suspend fun setWaveform(waveform: SynthWaveform) {
    this.waveform = waveform
  }

  override suspend fun setMasterVolume(volume: Float) {
    masterVolume = volume.coerceIn(0f, 1f)
  }

  internal suspend fun debugActiveVoiceCount(): Int = voiceMutex.withLock { voices.size }

  internal suspend fun debugRenderBlockForTest(size: Int = config.blockSize): FloatArray = renderBlock(size)

  private suspend fun renderBlock(size: Int): FloatArray {
    val output = FloatArray(size)

    voiceMutex.withLock {
      if (voices.isEmpty()) return output
      val voiceCount = voices.size

      repeat(size) { index ->
        var sample = 0f

        voices.forEach { voice ->
          sample += voice.nextSample(waveform = waveform, sampleRateHz = config.sampleRateHz, attackSeconds = config.attackSeconds, releaseSeconds = config.releaseSeconds)
        }

        // Normalize by polyphony and apply gentle soft-clipping to avoid harsh digital distortion.
        val normalized = sample / sqrt(voiceCount.toFloat())
        output[index] = softClip(normalized * config.outputGain)
      }

      voices.removeAll { it.currentAmplitude <= SILENCE_THRESHOLD }
    }

    return output
  }

  private suspend fun runRenderLoop() {
    val frameDurationMs = ((config.blockSize.toDouble() / config.sampleRateHz) * 1000.0).toLong().coerceAtLeast(1L)
    try {
      while (currentCoroutineContext().isActive) {
        val block = renderBlock(config.blockSize)
        audioOutput.writeMonoPcm(block)
        if (!audioOutput.blocksOnWrite) {
          delay(frameDurationMs)
        }
      }
    } catch (_: CancellationException) {
      // Expected on stop.
    } catch (t: Throwable) {
      lifecycleMutex.withLock {
        if (renderJob == null) return
        renderJob = null
        voiceMutex.withLock {
          voices.clear()
        }
        runCatching { audioOutput.stop() }
        _state.value = InstrumentEngineState(InstrumentEngineState.Status.FAILED, t.message ?: "Render loop failed")
      }
    }
  }

  data class SynthEngineConfig(
    val sampleRateHz: Int = 48_000,
    val blockSize: Int = 256,
    val maxVoices: Int = 16,
    val attackSeconds: Float = 0.005f,
    val releaseSeconds: Float = 0.08f,
    val outputGain: Float = 0.85f,
    val initialMasterVolume: Float = 1f,
  )

  private data class Voice(
    val note: Int,
    val channel: Int,
    val frequencyHz: Float,
    var targetAmplitude: Float,
    var currentAmplitude: Float,
    var phase: Float,
    val sequence: Long,
    var released: Boolean,
  ) {
    fun nextSample(
      waveform: SynthWaveform,
      sampleRateHz: Int,
      attackSeconds: Float,
      releaseSeconds: Float,
    ): Float {
      val attackStep = if (attackSeconds <= 0f) 1f else 1f / (attackSeconds * sampleRateHz)
      val releaseStep = if (releaseSeconds <= 0f) 1f else 1f / (releaseSeconds * sampleRateHz)

      if (released) {
        currentAmplitude = (currentAmplitude - releaseStep).coerceAtLeast(0f)
      } else {
        currentAmplitude = (currentAmplitude + attackStep).coerceAtMost(targetAmplitude)
      }

      val wave = sampleWave(waveform, phase)
      val out = wave * currentAmplitude

      phase += frequencyHz / sampleRateHz
      if (phase >= 1f) phase -= 1f

      return out
    }
  }

  private companion object {
    val MIDI_NOTE_RANGE = 0..127
    val MIDI_VALUE_RANGE = 0..127
    val MIDI_CHANNEL_RANGE = 0..15

    const val MIDI_MAX_FLOAT = 127f
    const val SILENCE_THRESHOLD = 0.0001f

    fun sampleWave(
      waveform: SynthWaveform,
      phase: Float,
    ): Float =
      when (waveform) {
        SynthWaveform.SINE -> kotlin.math.sin((2.0 * PI * phase).toFloat())
        SynthWaveform.TRIANGLE -> {
          val centeredPhase = phase - kotlin.math.floor((phase + 0.5f).toDouble()).toFloat()
          2f * abs(2f * centeredPhase) - 1f
        }
        SynthWaveform.SQUARE -> sign(kotlin.math.sin((2.0 * PI * phase).toFloat())).let { if (it == 0f) 1f else it }
        SynthWaveform.SAW -> (2f * phase) - 1f
      }

    fun midiNoteToFrequency(note: Int): Float = (440.0 * 2.0.pow((note - 69) / 12.0)).toFloat()

    fun softClip(x: Float): Float = x / (1f + abs(x))
  }
}
