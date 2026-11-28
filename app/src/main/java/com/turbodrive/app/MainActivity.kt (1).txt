package com.turbodrive.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.turbodrive.app.model.TelemetryState
import com.turbodrive.app.service.EngineForegroundService
import com.turbodrive.app.ui.DashboardScreen
import com.turbodrive.app.ui.theme.TurboDriveTheme

class MainActivity : ComponentActivity() {

    private var engineService by mutableStateOf<EngineForegroundService?>(null)
    private var isBound = false

    // Opens Android's file picker so the user can choose an audio file.
    private val soundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // The file can still be selected even if persistent access is unavailable.
                }

                Toast.makeText(
                    this,
                    "Sound file selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun openSoundPicker() {
        soundPickerLauncher.launch(arrayOf("audio/*"))
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as EngineForegroundService.LocalBinder
            engineService = binder.getService()
            isBound = true
            checkAndRequestPermissions()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            engineService = null
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineLocationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted) {
            engineService?.onPermissionGranted()
        } else {
            engineService?.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, EngineForegroundService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            TurboDriveTheme {
                val service = engineService
                val stateFlow = service?.telemetry
                val telemetryState = if (stateFlow != null) {
                    stateFlow.collectAsState().value
                } else {
                    TelemetryState()
                }

                DashboardScreen(
                    state = telemetryState,
                    onStartEngine = { service?.startEngine() },
                    onStopEngine = { service?.stopEngine() },
                    onToggleMute = { service?.setMuted(!telemetryState.isMuted) },
                    onIntensityChange = { service?.setSoundIntensity(it) },
                    onModeSelect = { service?.setSoundMode(it) },
                    onRequestPermissions = { checkAndRequestPermissions() },
                    onAddSound = { openSoundPicker() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            engineService?.onPermissionGranted()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
