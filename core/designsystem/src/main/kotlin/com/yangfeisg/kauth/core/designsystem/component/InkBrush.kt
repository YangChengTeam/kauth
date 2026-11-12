package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * An arc as one brush stroke: loaded with ink where it starts and thinning as it travels.
 * The taper follows the position on the full circle, so a shorter stroke is the same
 * stroke lifted earlier. [color] should be opaque, as the stroke is laid in overlapping dabs.
 */
fun DrawScope.drawBrushArc(
    color: Color,
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    maxWidth: Float,
) {
    if (sweepAngle <= 0f) return
    val steps = (sweepAngle / 4f).toInt().coerceAtLeast(1)
    val step = sweepAngle / steps
    val topLeft = center - Offset(radius, radius)
    val arcSize = Size(radius * 2, radius * 2)
    for (i in 0 until steps) {
        val travelled = (i + 0.5f) * step / 360f
        drawArc(
            color = color,
            startAngle = startAngle + i * step,
            sweepAngle = step,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = maxWidth * (1f - 0.62f * travelled), cap = StrokeCap.Round),
        )
    }
}
