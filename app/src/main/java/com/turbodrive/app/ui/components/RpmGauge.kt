package com.turbodrive.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.turbodrive.app.ui.theme.AmberShift
import com.turbodrive.app.ui.theme.RacingRed
import com.turbodrive.app.ui.theme.TextPrimary
import com.turbodrive.app.ui.theme.TextSecondary
import com.turbodrive.app.ui.theme.TurboCyan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RpmGauge(
    rpm: Float,
    speedKmh: Float,
    maxRpm: Float = 8500f,
    modifier: Modifier = Modifier
) {
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.coerceIn(0f, maxRpm),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rpm_needle"
    )

    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.coerceAtLeast(0f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "speed_readout"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 22.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val startAngle = 140f
            val sweepTotal = 260f

            // Background dark track
            drawArc(
                color = Color(0xFF1E2430),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active RPM sweep arc
            val progress = (animatedRpm / maxRpm).coerceIn(0f, 1f)
            val activeSweep = sweepTotal * progress
            val arcBrush = Brush.sweepGradient(
                0.0f to TurboCyan,
                0.65f to AmberShift,
                0.85f to RacingRed,
                1.0f to RacingRed
            )

            if (activeSweep > 0.5f) {
                drawArc(
                    brush = arcBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Major RPM tick marks (0 to 8.5k)
            val tickCount = 9
            for (i in 0 until tickCount) {
                val fraction = i / (tickCount - 1f)
                val angleDeg = startAngle + fraction * sweepTotal
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val innerR = radius - strokeWidth * 0.85f
                val outerR = radius + strokeWidth * 0.25f

                val start = Offset(
                    x = center.x + (innerR * cos(angleRad)).toFloat(),
                    y = center.y + (innerR * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (outerR * cos(angleRad)).toFloat(),
                    y = center.y + (outerR * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (i >= 7) RacingRed else Color(0xFF6B778D),
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Digital GPS Speedometer & RPM Telemetry
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GPS SPEED",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = animatedSpeed.roundToInt().toString(),
                color = TextPrimary,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "KM/H",
                color = TurboCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text(
                text = "${animatedRpm.roundToInt()} RPM",
                color = if (animatedRpm > 6800f) RacingRed else AmberShift,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
