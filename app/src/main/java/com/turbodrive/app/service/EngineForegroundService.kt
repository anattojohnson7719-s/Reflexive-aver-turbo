package com.turbodrive.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.turbodrive.app.MainActivity
import com.turbodrive.app.audio.BluetoothAudioRouter
import com.turbodrive.app.audio.RacingSoundEngine
import com.turbodrive.app.location.GpsSpeedManager
import com.turbodrive.app.model.GpsSignalStatus
import com.turbodrive.app.model.SoundMode
import com.turbodrive.app.model.TelemetryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EngineForegroundService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): EngineForegroundService = this@EngineForegroundService
    }

    private val binder = LocalBinder()

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private lateinit var gpsManager: GpsSpeedManager
    private lateinit var soundEngine: RacingSoundEngine
    private lateinit var bluetoothRouter: BluetoothAudioRouter

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        soundEngine = RacingSoundEngine(this)
        _telemetry.update { it.copy(usingCustomRawAssets = soundEngine.hasCustomRawAssets) }

        bluetoothRouter = BluetoothAudioRouter(
            context = this,
            onAudioRouteChanged = { isBt, deviceName ->
                _telemetry.update {
                    it.copy(isBluetoothConnected = isBt, audioOutputDeviceName = deviceName)
                }
            },
            onAudioFocusLost = {
                // Pause audio output gracefully if a phone call or navigation prompt interrupts
                setMuted(true)
            }
        )
        bluetoothRouter.startMonitoring()

        gpsManager = GpsSpeedManager(this) { rawKmh, smoothedKmh, accelMps2, accuracy, status ->
            val current = _telemetry.value
            val audioResult = soundEngine.updateTelemetry(
                speedKmh = smoothedKmh,
                accelerationMps2 = accelMps2,
                intensity = current.soundIntensity,
                isMuted = current.isMuted,
                mode = current.soundMode
            )

            _telemetry.update {
                it.copy(
                    speedKmh = rawKmh,
                    smoothedSpeedKmh = smoothedKmh,
                    accelerationMps2 = accelMps2,
                    simulatedRpm = if (it.isEngineRunning) audioResult.simulatedRpm else 0f,
                    turboBoostBar = if (it.isEngineRunning) audioResult.turboBoostBar else 0f,
                    bovTriggeredCount = if (audioResult.bovFired) it.bovTriggeredCount + 1 else it.bovTriggeredCount,
                    gpsAccuracyMeters = accuracy,
                    gpsStatus = status
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    fun onPermissionGranted() {
        gpsManager.startTracking()
    }

    fun onPermissionDenied() {
        _telemetry.update { it.copy(gpsStatus = GpsSignalStatus.NO_PERMISSION) }
    }

    fun startEngine() {
        val current = _telemetry.value
        if (current.isEngineRunning) return

        startForeground(NOTIFICATION_ID, buildNotification())
        bluetoothRouter.requestEngineAudioFocus()
        soundEngine.startEngine(current.soundIntensity, current.isMuted)
        gpsManager.startTracking()

        _telemetry.update {
            it.copy(
                isEngineRunning = true,
                simulatedRpm = 900f
            )
        }
    }

    fun stopEngine() {
        if (!_telemetry.value.isEngineRunning) return
        soundEngine.stopEngine()
        bluetoothRouter.abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)

        _telemetry.update {
            it.copy(
                isEngineRunning = false,
                simulatedRpm = 0f,
                turboBoostBar = 0f
            )
        }
    }

    fun setMuted(muted: Boolean) {
        _telemetry.update { it.copy(isMuted = muted) }
        refreshAudioImmediately()
    }

    fun setSoundIntensity(intensity: Float) {
        _telemetry.update { it.copy(soundIntensity = intensity.coerceIn(0f, 1f)) }
        refreshAudioImmediately()
    }

    fun setSoundMode(mode: SoundMode) {
        _telemetry.update { it.copy(soundMode = mode) }
        refreshAudioImmediately()
    }

    private fun refreshAudioImmediately() {
        val s = _telemetry.value
        if (s.isEngineRunning) {
            soundEngine.updateTelemetry(
                speedKmh = s.smoothedSpeedKmh,
                accelerationMps2 = s.accelerationMps2,
                intensity = s.soundIntensity,
                isMuted = s.isMuted,
                mode = s.soundMode
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TURBO DRIVE Telemetry & Audio",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps GPS speedometer and racing engine audio active"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TURBO DRIVE Active")
            .setContentText("GPS telemetry & racing engine simulator running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        gpsManager.stopTracking()
        bluetoothRouter.stopMonitoring()
        soundEngine.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "turbo_drive_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
