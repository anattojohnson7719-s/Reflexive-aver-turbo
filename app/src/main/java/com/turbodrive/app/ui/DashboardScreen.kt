package com.turbodrive.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turbodrive.app.model.GpsSignalStatus
import com.turbodrive.app.model.SoundMode
import com.turbodrive.app.model.TelemetryState
import com.turbodrive.app.ui.components.RpmGauge
import com.turbodrive.app.ui.components.TurboBoostGauge
import com.turbodrive.app.ui.theme.AmberShift
import com.turbodrive.app.ui.theme.CarbonElevated
import com.turbodrive.app.ui.theme.CarbonSurface
import com.turbodrive.app.ui.theme.EmeraldOk
import com.turbodrive.app.ui.theme.ObsidianBlack
import com.turbodrive.app.ui.theme.RacingRed
import com.turbodrive.app.ui.theme.TextPrimary
import com.turbodrive.app.ui.theme.TextSecondary
import com.turbodrive.app.ui.theme.TurboCyan
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: TelemetryState,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onToggleMute: () -> Unit,
    onIntensityChange: (Float) -> Unit,
    onModeSelect: (SoundMode) -> Unit,
    onRequestPermissions: () -> Unit,
    onAddSound: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header & Status Pills
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TURBO DRIVE",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = if (state.usingCustomRawAssets) "RAW SAMPLES" else "SYNTH ENGINE",
                    color = TurboCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CarbonElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GPS Signal & Bluetooth Audio Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val gpsColor = when (state.gpsStatus) {
                    GpsSignalStatus.LOCKED -> EmeraldOk
                    GpsSignalStatus.WEAK -> AmberShift
                    else -> RacingRed
                }

                // GPS Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonSurface)
                        .clickable {
                            if (state.gpsStatus == GpsSignalStatus.NO_PERMISSION) {
                                onRequestPermissions()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS Status",
                        tint = gpsColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = state.gpsStatus.label,
                            color = gpsColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (state.gpsAccuracyMeters > 0f) {
                                "±${state.gpsAccuracyMeters.roundToInt()}m ACC"
                            } else {
                                "WAIT FOR FIX"
                            },
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bluetooth Audio Indicator
                val btColor = if (state.isBluetoothConnected) TurboCyan else TextSecondary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonSurface)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothAudio,
                        contentDescription = "Audio Output",
                        tint = btColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (state.isBluetoothConnected) "BT CAR AUDIO" else "LOCAL AUDIO",
                            color = btColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.audioOutputDeviceName,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Main Animated RPM Gauge + Digital GPS Speedometer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            RpmGauge(
                rpm = state.simulatedRpm,
                speedKmh = state.smoothedSpeedKmh,
                modifier = Modifier.fillMaxWidth(0.86f)
            )
        }

        // Animated Turbo Boost Gauge
        TurboBoostGauge(
            boostBar = state.turboBoostBar,
            bovCount = state.bovTriggeredCount
        )

        // Sound Mode Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CarbonSurface)
                .padding(12.dp)
        ) {
            Text(
                text = "EXHAUST & ENGINE PROFILE",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoundMode.entries.forEach { mode ->
                    val selected = mode == state.soundMode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) RacingRed else CarbonElevated)
                            .clickable { onModeSelect(mode) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (selected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opens the audio picker when connected from MainActivity.
            Button(
                onClick = onAddSound,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CarbonElevated,
                    contentColor = TurboCyan
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    text = "+ ADD SOUND (.WAV / .OGG)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Master Volume / Sound Intensity Slider + Mute Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (state.isMuted) RacingRed else CarbonElevated)
                ) {
                    Icon(
                        imageVector = if (state.isMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Mute Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SOUND INTENSITY",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(state.soundIntensity * 100).roundToInt()}%",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = state.soundIntensity,
                        onValueChange = onIntensityChange,
                        colors = SliderDefaults.colors(
                            thumbColor = TurboCyan,
                            activeTrackColor = TurboCyan,
                            inactiveTrackColor = CarbonElevated
                        )
                    )
                }
            }
        }

        // Start Engine & Stop Engine Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartEngine,
                enabled = !state.isEngineRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldOk,
                    contentColor = ObsidianBlack,
                    disabledContainerColor = CarbonElevated,
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START ENGINE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Button(
                onClick = onStopEngine,
                enabled = state.isEngineRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RacingRed,
                    contentColor = Color.White,
                    disabledContainerColor = CarbonElevated,
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .border(
                        width = 1.dp,
                        color = if (state.isEngineRunning) RacingRed else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP ENGINE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
