package com.yangfeisg.kauth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangfeisg.kauth.core.designsystem.component.InkLandscape
import com.yangfeisg.kauth.core.designsystem.component.InkSeal
import com.yangfeisg.kauth.core.designsystem.component.drawBrushArc

private const val LAUNCH_MILLIS = 2400

/**
 * The opening scene, painted as it is watched: one brush stroke closes a circle, hills
 * rise out of the mist beneath a red sun, the name surfaces, and a seal comes down to
 * sign the work. A tap skips it.
 */
@Composable
fun LaunchScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(1f, tween(LAUNCH_MILLIS, easing = LinearEasing))
        currentOnFinished()
    }
    // How far along the stretch [from]..[to] of the scene we are, eased.
    fun act(from: Float, to: Float): Float =
        FastOutSlowInEasing.transform(((time.value - from) / (to - from)).coerceIn(0f, 1f))

    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
    ) {
        InkLandscape(Modifier.fillMaxSize(), rise = act(0.15f, 0.75f))
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-72).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(216.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val brushWidth = 15.dp.toPx()
                    drawBrushArc(
                        color = ink,
                        center = center,
                        radius = (size.minDimension - brushWidth) / 2,
                        startAngle = -105f,
                        sweepAngle = 328f * act(0f, 0.5f),
                        maxWidth = brushWidth,
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    letterSpacing = 2.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = act(0.38f, 0.62f)
                        translationY = (1f - alpha) * 14.dp.toPx()
                    },
                )
                // Comes down from above the paper, like a hand stamping it.
                InkSeal(
                    character = stringResource(R.string.launch_seal),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-14).dp, y = (-20).dp)
                        .graphicsLayer {
                            val pressed = act(0.62f, 0.76f)
                            alpha = pressed
                            scaleX = 1.8f - 0.8f * pressed
                            scaleY = scaleX
                        },
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.launch_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer { alpha = act(0.55f, 0.82f) },
            )
        }
    }
}
