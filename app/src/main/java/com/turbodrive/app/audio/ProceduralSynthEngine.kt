package com.turbodrive.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time 44.1 kHz PCM synthesizer that generates:
 * - Multi-cylinder firing order engine fundamental + 2nd/4th harmonics
 * - Sports exhaust rasp & deceleration burble
 * - High-frequency turbocharger impeller spool whistle
 * - Band-limited pneumatic blow-off valve ("PSSHH") transient
 */
class ProceduralSynthEngine {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    @Volatile private var isRunning = false
    private var renderThread: Thread? = null

    @Volatile var targetRpm: Float = 850f
    @Volatile var turboBoostBar: Float = 0f
    @Volatile var exhaustGain: Float = 0f
    @Volatile var decelBurbleGain: Float = 0f
    @Volatile var masterGain: Float = 0.85f
    @Volatile private var bovEnvelope: Float = 0f

    fun triggerBlowOffValve() {
        bovEnvelope = 1.0f
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        renderThread = Thread({
            val buffer = ShortArray(minBuffer / 2)
            var crankPhase = 0.0
            var turboPhase = 0.0
            var lpNoise = 0.0
            var hpNoise = 0.0
            var currentRpm = 850.0

            while (isRunning) {
                val desiredRpm = targetRpm.toDouble()
                val boost = turboBoostBar.toDouble().coerceIn(0.0, 2.0)
                val exhaust = exhaustGain.toDouble().coerceIn(0.0, 1.0)
                val decel = decelBurbleGain.toDouble().coerceIn(0.0, 1.0)
                val gain = masterGain.toDouble().coerceIn(0.0, 1.0)

                for (i in buffer.indices) {
                    // Smooth sample-by-sample RPM slewing
                    currentRpm += (desiredRpm - currentRpm) * 0.0008

                    // 4-stroke inline-6 / flat-6 firing frequency: (RPM / 60) * 3 cylinders per rev
                    val firingFreq = (currentRpm / 60.0) * 3.0
                    crankPhase += (2.0 * PI * firingFreq) / sampleRate
                    if (crankPhase > 2.0 * PI) crankPhase -= 2.0 * PI

                    // Engine harmonics (fundamental + octave + exhaust fifth)
                    val fundamental = sin(crankPhase) * 0.55
                    val secondHarmonic = sin(crankPhase * 2.0) * 0.30
                    val exhaustHarmonic = sin(crankPhase * 3.0) * (0.15 + exhaust * 0.35)

                    // White noise source for exhaust texture & BOV
                    val white = Random.nextDouble(-1.0, 1.0)
                    lpNoise = lpNoise * 0.92 + white * 0.08
                    hpNoise = white - lpNoise

                    // Deceleration exhaust crackle/burble
                    val crackle = if (decel > 0.1 && Random.nextDouble() < 0.004 * decel) {
                        lpNoise * 1.8 * decel
                    } else {
                        lpNoise * 0.25 * exhaust
                    }

                    // Turbocharger turbine whine (2.2 kHz to 7.8 kHz proportional to boost)
                    val turboFreq = 2200.0 + boost * 3200.0
                    turboPhase += (2.0 * PI * turboFreq) / sampleRate
                    if (turboPhase > 2.0 * PI) turboPhase -= 2.0 * PI
                    val turboWhistle = sin(turboPhase) * (boost * 0.16)

                    // Pneumatic Blow-Off Valve ("PSSHH") decay
                    var bovSample = 0.0
                    if (bovEnvelope > 0.001f) {
                        bovSample = hpNoise * bovEnvelope * 0.65
                        bovEnvelope *= 0.99982f // ~380ms realistic pneumatic release
                    }

                    val mixed = (fundamental + secondHarmonic + exhaustHarmonic +
                        crackle + turboWhistle + bovSample) * gain

                    val clamped = mixed.coerceIn(-0.98, 0.98)
                    buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                }

                try {
                    track.write(buffer, 0, buffer.size)
                } catch (_: Exception) {
                    break
                }
            }
        }, "TurboSynthAudioThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning = false
        renderThread?.join(300)
        renderThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
