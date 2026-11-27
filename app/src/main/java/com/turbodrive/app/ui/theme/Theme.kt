package com.turbodrive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBlack = Color(0xFF07080B)
val CarbonSurface = Color(0xFF11141B)
val CarbonElevated = Color(0xFF191E29)
val RacingRed = Color(0xFFFF2A3C)
val TurboCyan = Color(0xFF00E5FF)
val AmberShift = Color(0xFFFFAB00)
val EmeraldOk = Color(0xFF00E676)
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF8B95A5)

private val RacingDarkColorScheme = darkColorScheme(
    primary = RacingRed,
    secondary = TurboCyan,
    tertiary = AmberShift,
    background = ObsidianBlack,
    surface = CarbonSurface,
    surfaceVariant = CarbonElevated,
    onPrimary = Color.White,
    onSecondary = ObsidianBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun TurboDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RacingDarkColorScheme,
        content = content
    )
}
