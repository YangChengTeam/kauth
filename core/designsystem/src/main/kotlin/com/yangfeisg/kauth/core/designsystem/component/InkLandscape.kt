package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** One range of hills: where its foot sits and how tall it stands (fractions of the height), and how dark. */
private class Range(val base: Float, val height: Float, val alpha: Float, val frequency: Float, val phase: Float) {
    /** Ridge height in 0..1 at [u], the fraction of the way across. */
    fun ridge(u: Float): Float {
        val x = u * frequency + phase
        return (0.55f * sin(x) + 0.3f * sin(x * 2.3f + phase) + 0.15f * sin(x * 5.1f)) * 0.5f + 0.5f
    }
}

// Far to near: each nearer range is lower and darker.
private val Ranges = listOf(
    Range(base = 0.80f, height = 0.13f, alpha = 0.09f, frequency = 5.0f, phase = 0.4f),
    Range(base = 0.87f, height = 0.11f, alpha = 0.14f, frequency = 6.5f, phase = 2.1f),
    Range(base = 0.95f, height = 0.09f, alpha = 0.22f, frequency = 4.2f, phase = 4.0f),
)

/**
 * The scenery behind every screen: three ranges of distant hills washed in at the foot of
 * the page, each darkest along its ridge and dissolving into mist below, under a cinnabar sun.
 *
 * [rise] in 0..1 brings the scene in, far range first; it is 1 once settled.
 */
@Composable
fun InkLandscape(modifier: Modifier = Modifier, rise: Float = 1f) {
    val ink = MaterialTheme.colorScheme.onSurface
    val sun = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        drawCircle(
            color = sun.copy(alpha = 0.42f * rise),
            radius = 15.dp.toPx(),
            center = Offset(width * 0.24f, height * (0.70f + 0.05f * (1f - rise))),
        )
        Ranges.forEachIndexed { index, range ->
            val arrived = ((rise - index * 0.15f) / 0.7f).coerceIn(0f, 1f)
            val lift = (1f - arrived) * 48.dp.toPx()
            val path = Path().apply {
                moveTo(0f, height)
                for (i in 0..RIDGE_POINTS) {
                    val u = i / RIDGE_POINTS.toFloat()
                    lineTo(u * width, height * (range.base - range.height * range.ridge(u)) + lift)
                }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    0f to ink.copy(alpha = range.alpha * arrived),
                    1f to Color.Transparent,
                    startY = height * (range.base - range.height) + lift,
                    endY = height * (range.base + 0.08f) + lift,
                ),
            )
        }
    }
}

private const val RIDGE_POINTS = 64
