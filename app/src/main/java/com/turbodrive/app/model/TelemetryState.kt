package com.turbodrive.app.model

enum class SoundMode(val displayName: String, val rpmMultiplier: Float, val turboGain: Float) {
    STREET_SPORT("STREET", 1.0f, 0.85f),
    TRACK_GT3("TRACK GT3", 1.25f, 1.0f),
    TWIN_TURBO_JDM("JDM TURBO", 1.15f, 1.35f)
}

enum class GpsSignalStatus(val label: String) {
    NO_PERMISSION("NO GPS PERMISSION"),
    SEARCHING("ACQUIRING SATELLITES..."),
    WEAK("WEAK GPS SIGNAL"),
    LOCKED("GPS LOCKED")
}

data class TelemetryState(
    val isEngineRunning: Boolean = false,
    val speedKmh: Float = 0f,
    val smoothedSpeedKmh: Float = 0f,
    val accelerationMps2: Float = 0f,
    val simulatedRpm: Float = 0f,          // 0 when off, 850..8500 when running
    val turboBoostBar: Float = 0f,         // 0.0 to 1.8 BAR
    val bovTriggeredCount: Int = 0,
    val gpsAccuracyMeters: Float = 0f,
    val gpsStatus: GpsSignalStatus = GpsSignalStatus.SEARCHING,
    val isBluetoothConnected: Boolean = false,
    val audioOutputDeviceName: String = "Phone Speaker",
    val isMuted: Boolean = false,
    val soundIntensity: Float = 0.85f,     // 0.0 to 1.0
    val soundMode: SoundMode = SoundMode.TWIN_TURBO_JDM,
    val usingCustomRawAssets: Boolean = false
)
