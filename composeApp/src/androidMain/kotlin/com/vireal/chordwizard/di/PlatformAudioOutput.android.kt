package com.vireal.chordwizard.di

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import com.vireal.chordwizard.audio.AudioOutput
import com.vireal.chordwizard.audio.floatMonoToPcm16Le

actual fun createPlatformAudioOutput(): AudioOutput = AndroidAudioTrackOutput()

private class AndroidAudioTrackOutput : AudioOutput {
  private val lock = Any()
  private var audioTrack: AudioTrack? = null

  override val blocksOnWrite: Boolean = true

  override suspend fun start(
    sampleRateHz: Int,
    channels: Int,
  ) {
    require(channels == 1) { "Android audio output currently supports mono only, got channels=$channels" }

    synchronized(lock) {
      if (audioTrack != null) return

      val minBufferBytes = AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
      require(minBufferBytes > 0) { "Failed to query AudioTrack min buffer size: $minBufferBytes" }

      val requestedBufferBytes = calculateRequestedBufferBytes(sampleRateHz = sampleRateHz, channels = channels, minBufferBytes = minBufferBytes)
      val track = buildAudioTrack(sampleRateHz = sampleRateHz, channels = channels, bufferSizeBytes = requestedBufferBytes)

      if (track.state != AudioTrack.STATE_INITIALIZED) {
        runCatching { track.release() }
        throw IllegalStateException("AudioTrack is not initialized")
      }

      runCatching { track.play() }
        .onFailure {
          runCatching { track.release() }
          throw it
        }

      audioTrack = track
    }
  }

  override suspend fun writeMonoPcm(samples: FloatArray) {
    if (samples.isEmpty()) return
    val activeTrack = synchronized(lock) { audioTrack } ?: return

    val bytes = floatMonoToPcm16Le(samples)
    var offset = 0

    while (offset < bytes.size) {
      val written = writeBytes(activeTrack, bytes, offset, bytes.size - offset)
      if (written <= 0) {
        throw IllegalStateException("AudioTrack write failed: $written")
      }
      offset += written
    }
  }

  override suspend fun stop() {
    val trackToStop =
      synchronized(lock) {
        val active = audioTrack ?: return
        audioTrack = null
        active
      }

    runCatching { trackToStop.pause() }
    runCatching { trackToStop.flush() }
    runCatching { trackToStop.stop() }
    runCatching { trackToStop.release() }
  }

  private fun buildAudioTrack(
    sampleRateHz: Int,
    channels: Int,
    bufferSizeBytes: Int,
  ): AudioTrack {
    val channelMask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
    val builder =
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        ).setAudioFormat(
          AudioFormat.Builder()
            .setSampleRate(sampleRateHz)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(channelMask)
            .build(),
        ).setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(bufferSizeBytes)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
    }

    return builder.build()
  }

  private fun writeBytes(
    track: AudioTrack,
    data: ByteArray,
    offset: Int,
    length: Int,
  ): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      track.write(data, offset, length, AudioTrack.WRITE_BLOCKING)
    } else {
      @Suppress("DEPRECATION")
      track.write(data, offset, length)
    }

  private companion object {
    const val TARGET_OUTPUT_BUFFER_MS = 24
    const val BYTES_PER_SAMPLE = 2

    fun calculateRequestedBufferBytes(
      sampleRateHz: Int,
      channels: Int,
      minBufferBytes: Int,
    ): Int {
      val bytesPerFrame = channels * BYTES_PER_SAMPLE
      val targetFrames = (sampleRateHz * TARGET_OUTPUT_BUFFER_MS) / 1000
      val targetBytes = maxOf(targetFrames, 256) * bytesPerFrame
      return maxOf(targetBytes, minBufferBytes)
    }
  }
}
