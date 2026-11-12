package com.yangfeisg.kauth.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangfeisg.kauth.core.designsystem.icon.KauthIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

/**
 * The add button: a drop of ink, never quite still. Opened, it turns to seal red, its plus
 * to a cross, and the two ways of adding an account slide out as slips of paper onto the
 * ink that spreads behind them.
 */
@Composable
internal fun AddAccountFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onScanQr: () -> Unit,
    onEnterKey: () -> Unit,
    onPositioned: (center: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        MenuSlip(expanded, order = 1, KauthIcons.Keyboard, stringResource(R.string.home_enter_key), onEnterKey)
        MenuSlip(expanded, order = 0, KauthIcons.Camera, stringResource(R.string.home_scan_qr), onScanQr)
        InkDrop(
            expanded = expanded,
            onClick = onToggle,
            modifier = Modifier.onGloballyPositioned { onPositioned(it.boundsInRoot().center) },
        )
    }
}

@Composable
private fun InkDrop(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, bouncy, label = "press")
    val rotation by animateFloatAsState(if (expanded) 135f else 0f, bouncy, label = "rotation")
    val color by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "drop",
    )
    val contentColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
        label = "content",
    )
    val wobble by rememberInfiniteTransition(label = "wobble").animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "phase",
    )
    val description = stringResource(if (expanded) R.string.home_close_menu else R.string.home_add_code)

    Box(
        modifier = modifier
            .size(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            // A circle whose edge swells and settles, in whole cycles so the loop has no seam.
            val base = size.minDimension / 2 * 0.86f
            val outline = Path()
            for (i in 0..DROP_POINTS) {
                val angle = i * TWO_PI / DROP_POINTS
                val radius = base * (1f + 0.04f * sin(3 * angle + wobble) + 0.025f * sin(5 * angle - 2 * wobble))
                val point = center + Offset(cos(angle) * radius, sin(angle) * radius)
                if (i == 0) outline.moveTo(point.x, point.y) else outline.lineTo(point.x, point.y)
            }
            outline.close()
            drawPath(outline, color)
        }
        Icon(
            painter = painterResource(KauthIcons.Add),
            contentDescription = description,
            tint = contentColor,
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation),
        )
    }
}

/** A menu choice on a slip of paper; [order] counts up from the add button and staggers the entrance. */
@Composable
private fun MenuSlip(visible: Boolean, order: Int, icon: Int, label: String, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, delayMillis = 140 + order * 80)) +
            slideInHorizontally(tween(420, delayMillis = 140 + order * 80)) { it / 2 },
        exit = fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { it / 3 },
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 14.dp, end = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private const val DROP_POINTS = 72
