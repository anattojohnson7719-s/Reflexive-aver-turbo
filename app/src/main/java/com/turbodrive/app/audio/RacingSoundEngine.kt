package com.turbodrive.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.turbodrive.app.model.SoundMode
import kotlin.math.abs

/**
 * Coordinates speed-based and acceleration-based racing audio.
 * Automatically uses custom recordings in res/raw/ if present:
 *  - engine_idle.wav / .ogg
 *  - engine_low_rpm.wav / .ogg
 *  - engine_high_rpm.wav / .ogg
 *  - turbo_spool.wav / .ogg
 *  - blowoff_valve.wav / .ogg
 *  - sports_exhaust.wav / .ogg
 *  - engine_decel.wav / .ogg
 * Falls back seamlessly to ProceduralSynthEngine when res/raw/ files are absent.
 */
class RacingSoundEngine(private val context: Context) {

    private val soundPool: SoundPool
    private val synthEngine = ProceduralSynthEngine()

    private var idleSoundId = 0
    private var lowRpmSoundId = 0
    private var highRpmSoundId = 0
    private var turboSpoolSoundId = 0
    private var bovSoundId = 0
    private var exhaustSoundId = 0
    private var decelSoundId = 0

    private var idleStreamId = 0
    private var lowRpmStreamId = 0
    private var highRpmStreamId = 0
    private var turboStreamId = 0
    private var exhaustStreamId = 0
    private var decelStreamId = 0

    val hasCustomRawAssets: Boolean
    private var isEngineStarted = false
    private var lastBovTriggerMs = 0L
    private var wasAcceleratingHard = false
    private var peakBoostBeforeLift = 0f

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()

        idleSoundId = loadRawIfExists("engine_idle")
        lowRpmSoundId = loadRawIfExists("engine_low_rpm")
        highRpmSoundId = loadRawIfExists("engine_high_rpm")
        turboSpoolSoundId = loadRawIfExists("turbo_spool")
        bovSoundId = loadRawIfExists("blowoff_valve")
        exhaustSoundId = loadRawIfExists("sports_exhaust")
        decelSoundId = loadRawIfExists("engine_decel")

        hasCustomRawAssets = (idleSoundId != 0 && lowRpmSoundId != 0 && highRpmSoundId != 0)
    }

    private fun loadRawIfExists(resourceName: String): Int {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        return if (resId != 0) soundPool.load(context, resId, 1) else 0
    }

    fun startEngine(initialIntensity: Float, isMuted: Boolean) {
        if (isEngineStarted) return
        isEngineStarted = true
        val effectiveVol = if (isMuted) 0f else initialIntensity

        if (hasCustomRawAssets) {
            idleStreamId = loopSample(idleSoundId, effectiveVol * 0.8f, 1.0f)
            lowRpmStreamId = loopSample(lowRpmSoundId, 0f, 0.85f)
            highRpmStreamId = loopSample(highRpmSoundId, 0f, 0.85f)
            turboStreamId = loopSample(turboSpoolSoundId, 0f, 0.9f)
            exhaustStreamId = loopSample(exhaustSoundId, 0f, 0.9f)
            decelStreamId = loopSample(decelSoundId, 0f, 1.0f)
        } else {
            synthEngine.masterGain = effectiveVol
            synthEngine.targetRpm = 900f
            synthEngine.start()
        }
    }

    fun stopEngine() {
        if (!isEngineStarted) return
        isEngineStarted = false

        listOf(
            idleStreamId, lowRpmStreamId, highRpmStreamId,
            turboStreamId, exhaustStreamId, decelStreamId
        ).forEach { streamId ->
            if (streamId != 0) soundPool.stop(streamId)
        }
        idleStreamId = 0
        lowRpmStreamId = 0
        highRpmStreamId = 0
        turboStreamId = 0
        exhaustStreamId = 0
        decelStreamId = 0

        synthEngine.stop()
    }

    /**
     * Evaluates speed zones (0-20, 20-40, 40-60, 60-80, 80+ km/h) and real-time acceleration.
     * Returns TelemetryAudioResult(simulatedRpm, turboBoostBar, bovFired).
     */
    fun updateTelemetry(
        speedKmh: Float,
        accelerationMps2: Float,
        intensity: Float,
        isMuted: Boolean,
        mode: SoundMode
    ): TelemetryAudioResult {
        if (!isEngineStarted) {
            return TelemetryAudioResult(0f, 0f, false)
        }

        val masterVol = if (isMuted) 0f else intensity.coerceIn(0f, 1f)
        val isAccelerating = accelerationMps2 > 0.65f
        val isDecelerating = accelerationMps2 < -0.75f

        // Simulated multi-gear transmission RPM curve based on GPS speed + throttle load
        val gearRatioFactor = when {
            speedKmh < 20f -> 115f
            speedKmh < 40f -> 95f
            speedKmh < 60f -> 82f
            speedKmh < 80f -> 72f
            else -> 64f
        }
        val accelRpmBoost = (accelerationMps2.coerceIn(0f, 5f)) * 420f
        val rawRpm = (850f + (speedKmh * gearRatioFactor) + accelRpmBoost) * mode.rpmMultiplier
        val simulatedRpm = rawRpm.coerceIn(850f, 8500f)

        // Turbo boost calculation (0.0 to 1.8 BAR)
        val speedBoost = ((speedKmh - 35f) / 65f).coerceIn(0f, 1.0f)
        val accelBoost = if (isAccelerating) (accelerationMps2 / 3.5f).coerceIn(0f, 0.9f) else 0f
        val turboBoostBar = ((speedBoost * 0.8f + accelBoost) * mode.turboGain).coerceIn(0f, 1.8f)

        // Detect throttle lift-off / significant deceleration to trigger Blow-Off Valve (PSSHH)
        var bovFired = false
        val nowMs = SystemClock.elapsedRealtime()
        if (isAccelerating && turboBoostBar > 0.55f) {
            wasAcceleratingHard = true
            peakBoostBeforeLift = maxOf(peakBoostBeforeLift, turboBoostBar)
        } else if (wasAcceleratingHard && (accelerationMps2 < 0.15f || isDecelerating)) {
            if (nowMs - lastBovTriggerMs > 1800L && peakBoostBeforeLift > 0.55f) {
                triggerBlowOffValve(masterVol)
                lastBovTriggerMs = nowMs
                bovFired = true
            }
            wasAcceleratingHard = false
            peakBoostBeforeLift = 0f
        }

        // Speed zone crossfades
        val idleGain: Float
        val lowRpmGain: Float
        val highRpmGain: Float
        val turboGain: Float
        val exhaustGain: Float
        val decelGain: Float = if (isDecelerating && speedKmh > 12f) {
            (abs(accelerationMps2) / 3.5f).coerceIn(0.2f, 1.0f)
        } else {
            0f
        }

        when {
            // 0-20 km/h: Low engine idle and revving
            speedKmh < 20f -> {
                val t = (speedKmh / 20f).coerceIn(0f, 1f)
                idleGain = (1f - t * 0.5f)
                lowRpmGain = t * 0.75f + (if (isAccelerating) 0.2f else 0f)
                highRpmGain = 0f
                turboGain = if (isAccelerating) 0.15f else 0f
                exhaustGain = t * 0.25f
            }
            // 20-40 km/h: Increase engine pitch and volume smoothly
            speedKmh < 40f -> {
                val t = ((speedKmh - 20f) / 20f).coerceIn(0f, 1f)
                idleGain = (0.5f * (1f - t)).coerceAtLeast(0f)
                lowRpmGain = 0.75f + t * 0.25f
                highRpmGain = t * 0.35f
                turboGain = if (isAccelerating) 0.25f else t * 0.15f
                exhaustGain = 0.25f + t * 0.2f
            }
            // 40-60 km/h: Add turbo spool sound
            speedKmh < 60f -> {
                val t = ((speedKmh - 40f) / 20f).coerceIn(0f, 1f)
                idleGain = 0f
                lowRpmGain = 1.0f - t * 0.35f
                highRpmGain = 0.35f + t * 0.35f
                turboGain = (0.35f + t * 0.35f + (if (isAccelerating) 0.25f else 0f)).coerceAtMost(1f)
                exhaustGain = 0.45f + t * 0.2f
            }
            // 60-80 km/h: Stronger turbo whistle and racing engine sound
            speedKmh < 80f -> {
                val t = ((speedKmh - 60f) / 20f).coerceIn(0f, 1f)
                idleGain = 0f
                lowRpmGain = 0.65f * (1f - t * 0.5f)
                highRpmGain = 0.70f + t * 0.25f
                turboGain = (0.70f + t * 0.25f).coerceAtMost(1f)
                exhaustGain = 0.65f + t * 0.2f
            }
            // 80+ km/h: Higher simulated RPM and sports exhaust sound
            else -> {
                idleGain = 0f
                lowRpmGain = 0.25f
                highRpmGain = 1.0f
                turboGain = 0.95f
                exhaustGain = 1.0f
            }
        }

        if (hasCustomRawAssets) {
            val pitchRatio = (0.75f + (simulatedRpm / 8500f) * 1.15f).coerceIn(0.6f, 2.0f)
            val turboPitch = (0.85f + (turboBoostBar / 1.8f) * 0.95f).coerceIn(0.7f, 2.0f)

            updateStream(idleStreamId, idleGain * masterVol, 0.9f + (speedKmh / 100f))
            updateStream(lowRpmStreamId, lowRpmGain * masterVol, pitchRatio * 0.92f)
            updateStream(highRpmStreamId, highRpmGain * masterVol, pitchRatio)
            updateStream(turboStreamId, turboGain * masterVol, turboPitch)
            updateStream(exhaustStreamId, exhaustGain * masterVol, pitchRatio * 0.96f)
            updateStream(decelStreamId, decelGain * masterVol, pitchRatio * 0.85f)
        } else {
            synthEngine.masterGain = masterVol
            synthEngine.targetRpm = simulatedRpm
            synthEngine.turboBoostBar = turboBoostBar
            synthEngine.exhaustGain = exhaustGain
            synthEngine.decelBurbleGain = decelGain
        }

        return TelemetryAudioResult(simulatedRpm, turboBoostBar, bovFired)
    }

    private fun triggerBlowOffValve(masterVol: Float) {
        if (masterVol <= 0.01f) return
        if (hasCustomRawAssets && bovSoundId != 0) {
            soundPool.play(bovSoundId, masterVol, masterVol, 2, 0, 1.0f)
        } else {
            synthEngine.triggerBlowOffValve()
        }
    }

    private fun loopSample(soundId: Int, volume: Float, rate: Float): Int {
        if (soundId == 0) return 0
        return soundPool.play(soundId, volume, volume, 1, -1, rate)
    }

    private fun updateStream(streamId: Int, volume: Float, rate: Float) {
        if (streamId == 0) return
        val v = volume.coerceIn(0f, 1f)
        soundPool.setVolume(streamId, v, v)
        soundPool.setRate(streamId, rate.coerceIn(0.5f, 2.0f))
    }

    fun release() {
        stopEngine()
        soundPool.release()
    }

    data class TelemetryAudioResult(
        val simulatedRpm: Float,
        val turboBoostBar: Float,
        val bovFired: Boolean
    )
}
