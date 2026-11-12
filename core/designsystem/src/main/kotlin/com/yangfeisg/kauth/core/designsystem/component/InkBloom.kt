package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()
private const val LOBES = 7
private const val DROPLETS = 7

/**
 * Ink spreading across the page as [spread] goes 0..1: a soft-edged blot of overlapping
 * lobes that keeps breathing while open, throwing off a few droplets on the way.
 *
 * It starts from [origin] (the centre if null) and at full spread reaches [reach] of the
 * page's diagonal; 1 from the centre floods the page.
 */
@Composable
fun InkBloom(
    spread: () -> Float,
    modifier: Modifier = Modifier,
    origin: Offset? = null,
    reach: Float = 0.64f,
    opacity: Float = 0.95f,
) {
    val ink = MaterialTheme.colorScheme.scrim
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.graphicsLayer { alpha = opacity }) {
        val amount = spread()
        if (amount <= 0.001f) return@Canvas
        val from = origin ?: center
        val extent = hypot(size.width, size.height) * reach * amount
        for (i in 0 until LOBES) {
            val angle = i * TWO_PI / LOBES + 0.6f
            val sway = sin(drift + i * 1.7f)
            val lobeCenter = from + Offset(cos(angle), sin(angle)) * (extent * (0.15f + 0.04f * sway))
            val lobeRadius = extent * (0.90f + 0.06f * sin(drift * 2 + i))
            drawCircle(
                brush = Brush.radialGradient(
                    0f to ink,
                    0.78f to ink,
                    0.90f to ink.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                    center = lobeCenter,
                    radius = lobeRadius.coerceAtLeast(1f),
                ),
                radius = lobeRadius,
                center = lobeCenter,
            )
        }
        // Droplets flung ahead of the edge; they show only while the ink is on the move.
        val flying = (amount * (1f - amount) * 4f).coerceIn(0f, 1f)
        if (flying > 0.02f) {
            for (i in 0 until DROPLETS) {
                val angle = PI.toFloat() * (1.05f + 0.4f * i / (DROPLETS - 1))
                val distance = extent * (1.02f + 0.05f * (i % 3))
                drawCircle(
                    color = ink.copy(alpha = flying),
                    radius = (3 + (i * 7) % 6).dp.toPx(),
                    center = from + Offset(cos(angle), sin(angle)) * distance,
                )
            }
        }
    }
}
