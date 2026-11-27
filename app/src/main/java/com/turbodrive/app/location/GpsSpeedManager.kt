package com.turbodrive.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.turbodrive.app.model.GpsSignalStatus

class GpsSpeedManager(
    context: Context,
    private val onGpsUpdate: (
        rawSpeedKmh: Float,
        smoothedSpeedKmh: Float,
        accelerationMps2: Float,
        accuracyMeters: Float,
        status: GpsSignalStatus
    ) -> Unit
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLocation: Location? = null
    private var lastTimestampMs: Long = 0L
    private var lastSpeedMps: Float = 0f
    private var emaSpeedKmh: Float = 0f
    private var isTracking = false

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        250L // 4 Hz update rate for responsive acceleration detection
    ).apply {
        setMinUpdateIntervalMillis(150L)
        setWaitForAccurateLocation(true)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            processLocation(location)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true
        lastLocation = null
        lastTimestampMs = 0L
        emaSpeedKmh = 0f
        onGpsUpdate(0f, 0f, 0f, 0f, GpsSignalStatus.SEARCHING)
        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun processLocation(location: Location) {
        val accuracy = if (location.hasAccuracy()) location.accuracy else 999f
        val status = when {
            accuracy <= 15f -> GpsSignalStatus.LOCKED
            accuracy <= 40f -> GpsSignalStatus.WEAK
            else -> GpsSignalStatus.SEARCHING
        }

        val nowMs = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()

        // Use hardware Doppler speed when available; fallback to real displacement/time
        var currentSpeedMps = if (location.hasSpeed()) {
            location.speed
        } else {
            val prev = lastLocation
            val dtSec = (nowMs - lastTimestampMs) / 1000f
            if (prev != null && dtSec > 0.05f) {
                prev.distanceTo(location) / dtSec
            } else {
                0f
            }
        }

        // Filter out stationary GPS drift (< 1.2 km/h when accuracy is loose)
        if (currentSpeedMps < 0.35f || accuracy > 35f) {
            currentSpeedMps = 0f
        }

        val rawSpeedKmh = currentSpeedMps * 3.6f

        // Smooth speed with Exponential Moving Average (EMA) for needle fluidity
        val alpha = 0.38f
        emaSpeedKmh = if (emaSpeedKmh == 0f && rawSpeedKmh > 0f) {
            rawSpeedKmh
        } else {
            emaSpeedKmh + alpha * (rawSpeedKmh - emaSpeedKmh)
        }
        if (emaSpeedKmh < 0.2f) emaSpeedKmh = 0f

        // Compute real longitudinal acceleration (m/s^2)
        val dtSec = if (lastTimestampMs > 0L) (nowMs - lastTimestampMs) / 1000f else 0f
        val accelerationMps2 = if (dtSec in 0.05f..3.0f) {
            (currentSpeedMps - lastSpeedMps) / dtSec
        } else {
            0f
        }

        lastLocation = location
        lastTimestampMs = nowMs
        lastSpeedMps = currentSpeedMps

        onGpsUpdate(rawSpeedKmh, emaSpeedKmh, accelerationMps2, accuracy, status)
    }
}
