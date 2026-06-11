package com.example

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.concurrent.thread
import kotlin.math.sin
import kotlin.random.Random

object AmbientAudioEngine {
    private const val TAG = "AmbientAudioEngine"
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var curIndex = -1
    private var volume = 0.7f
    private var audioThread: Thread? = null

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.let {
                it.setVolume(volume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    @Suppress("DEPRECATION")
    fun start(index: Int) {
        stop()
        curIndex = index
        isPlaying = true
        Log.d(TAG, "Starting audio synthesizer for track: $index")
        
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2,
                AudioTrack.MODE_STREAM
            )
            audioTrack = track
            track.setVolume(volume)
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
            return
        }

        audioThread = thread(start = true, name = "AmbientSynthThread") {
            val genBuffer = ShortArray(1024) // Stereo (512 frames)
            var phase = 0.0
            var phase2 = 0.0
            
            // Filters for cozy warm pink/brown noise
            var prevLeft = 0.0
            var prevRight = 0.0

            // Slowly oscillating LFO for waves and celestial volume swell
            var tidePhase = 0.0

            // Forest bird chirp parameters
            var chirpActive = false
            var chirpLength = 0
            var chirpCounter = 0
            var chirpFreqStart = 1500.0
            var chirpFreqEnd = 2500.0
            var nextChirpIn = 100

            while (isPlaying && curIndex == index) {
                for (i in 0 until genBuffer.size step 2) {
                    var left = 0.0
                    var right = 0.0

                    when (index) {
                        0 -> { // Cozy Rain Café: Pink/brown noise generator with raindrop impact crackles
                            val whiteLeft = Random.nextDouble() * 2.0 - 1.0
                            val whiteRight = Random.nextDouble() * 2.0 - 1.0
                            
                            // Low-pass filter noise to give it that warm, cozy rain-falling atmosphere
                            prevLeft = 0.06 * whiteLeft + 0.94 * prevLeft
                            prevRight = 0.06 * whiteRight + 0.94 * prevRight
                            
                            left = prevLeft * 0.7
                            right = prevRight * 0.7

                            // Sudden random realistic water droplets hitting surfaces
                            if (Random.nextDouble() < 0.00015) {
                                left += 0.35
                                right += 0.2
                            }
                        }
                        1 -> { // Cosmic Binaural Waves: Brainwave focus delta beat (150Hz left vs 154Hz right)
                            val freqL = 150.0
                            val freqR = 154.0
                            left = sin(phase)
                            right = sin(phase2)

                            phase += 2.0 * Math.PI * freqL / sampleRate
                            phase2 += 2.0 * Math.PI * freqR / sampleRate

                            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                            if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

                            // Ambient sweeping LFO volume swell (0.12 Hz)
                            val swell = 0.5 + 0.5 * sin(tidePhase)
                            tidePhase += 2.0 * Math.PI * 0.12 / sampleRate
                            if (tidePhase > 2.0 * Math.PI) tidePhase -= 2.0 * Math.PI

                            left *= swell * 0.4
                            right *= swell * 0.4
                        }
                        2 -> { // Forest Stream: Dynamic water murmur (modulated noise) + cute bird chirps
                            val whiteLeft = Random.nextDouble() * 2.0 - 1.0
                            val whiteRight = Random.nextDouble() * 2.0 - 1.0

                            // Flow gurgle/murmur LFO modulation
                            val gurgleMod = 0.75 + 0.25 * sin(phase)
                            phase += 2.0 * Math.PI * 1.8 / sampleRate 
                            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                            prevLeft = 0.035 * whiteLeft + 0.965 * prevLeft
                            prevRight = 0.035 * whiteRight + 0.965 * prevRight

                            left = prevLeft * gurgleMod * 0.65
                            right = prevRight * gurgleMod * 0.65

                            // Realistic spontaneous synthetic bird chirps!
                            if (!chirpActive) {
                                nextChirpIn--
                                if (nextChirpIn <= 0) {
                                    chirpActive = true
                                    chirpLength = (sampleRate * 0.16).toInt() // short, cheerful chirp
                                    chirpCounter = 0
                                    chirpFreqStart = 1600.0 + Random.nextDouble() * 600.0
                                    chirpFreqEnd = chirpFreqStart + 350.0 + Random.nextDouble() * 300.0
                                }
                            } else {
                                val t = chirpCounter.toDouble() / chirpLength.toDouble()
                                val currentFreq = chirpFreqStart + (chirpFreqEnd - chirpFreqStart) * t
                                val envelope = 0.22 * sin(Math.PI * t) // humped smooth sound envelope
                                
                                val chirpSample = sin(phase2)
                                phase2 += 2.0 * Math.PI * currentFreq / sampleRate
                                if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

                                left += chirpSample * envelope
                                right += chirpSample * envelope

                                chirpCounter++
                                if (chirpCounter >= chirpLength) {
                                    chirpActive = false
                                    nextChirpIn = sampleRate * (1 + Random.nextInt(4)) // Next bird in 1-5 seconds
                                }
                            }
                        }
                        3 -> { // Dream State Zen Waves: Gentle realistic ocean waves (periodic noise wash)
                            val whiteLeft = Random.nextDouble() * 2.0 - 1.0
                            val whiteRight = Random.nextDouble() * 2.0 - 1.0

                            prevLeft = 0.04 * whiteLeft + 0.96 * prevLeft
                            prevRight = 0.04 * whiteRight + 0.96 * prevRight

                            // 8-second wave tide period
                            val waveSwell = 0.05 + 0.95 * (sin(tidePhase) * 0.5 + 0.5)
                            tidePhase += 2.0 * Math.PI * 0.125 / sampleRate
                            if (tidePhase > 2.0 * Math.PI) tidePhase -= 2.0 * Math.PI

                            left = prevLeft * waveSwell * 0.7
                            right = prevRight * waveSwell * 0.7
                        }
                    }

                    // Write 16-bit PCM sound samples
                    val sampleL = (left * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    val sampleR = (right * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

                    genBuffer[i] = sampleL
                    genBuffer[i+1] = sampleR
                }
                
                try {
                    audioTrack?.write(genBuffer, 0, genBuffer.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Write error in audio generation loop", e)
                    break
                }
            }
        }
    }

    fun stop() {
        isPlaying = false
        curIndex = -1
        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // Safe ignore
        }
        audioTrack = null
        audioThread = null
    }
}
