package com.turbodrive.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class BluetoothAudioRouter(
    private val context: Context,
    private val onAudioRouteChanged: (isBluetoothConnected: Boolean, deviceName: String) -> Unit,
    private val onAudioFocusLost: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isRegistered = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            evaluateActiveOutputDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            evaluateActiveOutputDevice()
        }
    }

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            evaluateActiveOutputDevice()
        }
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onAudioFocusLost()
        }
    }

    fun startMonitoring() {
        if (isRegistered) return
        isRegistered = true
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        context.registerReceiver(
            scoReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )
        evaluateActiveOutputDevice()
    }

    fun stopMonitoring() {
        if (!isRegistered) return
        isRegistered = false
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        try {
            context.unregisterReceiver(scoReceiver)
        } catch (_: IllegalArgumentException) {
        }
        abandonAudioFocus()
    }

    fun requestEngineAudioFocus(): Boolean {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        audioFocusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
    }

    fun evaluateActiveOutputDevice() {
        // Ensure standard media/game routing so A2DP car stereo receives high-bandwidth stereo audio
        audioManager.mode = AudioManager.MODE_NORMAL
        if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }

        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btDevice = outputs.firstOrNull { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }

        if (btDevice != null) {
            val name = btDevice.productName?.toString()?.takeIf { it.isNotBlank() }
                ?: "Car Bluetooth Audio"
            onAudioRouteChanged(true, name)
        } else {
            val wired = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_AUX_LINE ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
            }
            val fallbackName = wired?.productName?.toString()?.takeIf { it.isNotBlank() }
                ?: "Phone Speaker"
            onAudioRouteChanged(false, fallbackName)
        }
    }
}
