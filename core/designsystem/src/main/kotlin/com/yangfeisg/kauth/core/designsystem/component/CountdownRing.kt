package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A brushed circle that shortens over each [periodSeconds] window of wall-clock time, with
 * the seconds left in its centre: how long the current time based code remains valid.
 */
@Composable
fun CountdownRing(
    periodSeconds: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val nowMillis by produceState(System.currentTimeMillis()) {
        while (true) withFrameMillis { value = System.currentTimeMillis() }
    }
    // Derived, so the number recomposes once a second while the ring redraws every frame.
    val secondsLeft by remember(periodSeconds) {
        derivedStateOf { periodSeconds - (nowMillis / 1000 % periodSeconds).toInt() }
    }
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val brushWidth = 4.5.dp.toPx()
            val radius = (size.minDimension - brushWidth) / 2
            val periodMillis = periodSeconds * 1000L
            val remaining = 1f - (nowMillis % periodMillis) / periodMillis.toFloat()
            drawCircle(color.copy(alpha = 0.12f), radius, style = Stroke(1.dp.toPx()))
            drawBrushArc(color, center, radius, startAngle = -90f, sweepAngle = 360f * remaining, maxWidth = brushWidth)
        }
        Text(
            text = secondsLeft.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = color,
        )
    }
}
