package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

private const val UNROLL_MILLIS = 560
private const val ROLL_UP_MILLIS = 280

/** What an [InkDialog]'s content can do: close it, rolling the scroll up first. */
interface InkDialogScope {
    /** Rolls the scroll up, then reports the dialog dismissed and runs [then]. */
    fun dismiss(then: () -> Unit = {})
}

/**
 * A dialog that opens like a hanging scroll: a rod appears, the paper unrolls beneath it
 * behind a second rod, and a brush stroke is drawn under the title. Closing rolls it back
 * up before [onDismissRequest] is called, so callers keep showing it until then.
 *
 * Buttons and content get an [InkDialogScope]; act, then call `dismiss()` to close.
 */
@Composable
fun InkDialog(
    onDismissRequest: () -> Unit,
    title: String,
    buttons: @Composable InkDialogScope.() -> Unit,
    content: (@Composable InkDialogScope.() -> Unit)? = null,
) {
    val unroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dialogScope = remember {
        object : InkDialogScope {
            private var closing = false

            override fun dismiss(then: () -> Unit) {
                if (closing) return
                closing = true
                scope.launch {
                    unroll.animateTo(0f, tween(ROLL_UP_MILLIS, easing = FastOutSlowInEasing))
                    onDismissRequest()
                    then()
                }
            }
        }
    }
    LaunchedEffect(Unit) { unroll.animateTo(1f, tween(UNROLL_MILLIS, easing = FastOutSlowInEasing)) }

    val ink = MaterialTheme.colorScheme.onSurface
    Dialog(
        onDismissRequest = { dialogScope.dismiss() },
        // Full width, so the scroll's rods may reach past the paper; the default dim stays.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { dialogScope.dismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 380.dp)
                    // Taps on the paper stay on the paper.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .graphicsLayer {
                        // The rods ink in first and fade last.
                        alpha = (unroll.value * 5f).coerceAtMost(1f)
                        translationY = (1f - unroll.value) * -18.dp.toPx()
                    }
                    .drawWithContent {
                        val edge = size.height * unroll.value
                        clipRect(bottom = edge) { this@drawWithContent.drawContent() }
                        drawRod(ink, y = 0f)
                        drawRod(ink, y = edge)
                    },
            ) {
                Column(
                    modifier = Modifier.padding(start = 28.dp, top = 30.dp, end = 20.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.drawBehind {
                            // The stroke follows the paper down, then is drawn left to right.
                            val drawn = ((unroll.value - 0.45f) / 0.55f).coerceIn(0f, 1f)
                            if (drawn > 0f) {
                                val y = size.height + 6.dp.toPx()
                                val length = 72.dp.toPx() * drawn
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        0f to ink,
                                        1f to ink.copy(alpha = 0.15f),
                                        startX = 0f,
                                        endX = length,
                                    ),
                                    start = Offset(0f, y),
                                    end = Offset(length, y),
                                    strokeWidth = 3.dp.toPx(),
                                )
                            }
                        },
                    )
                    content?.invoke(dialogScope)
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        content = { dialogScope.buttons() },
                    )
                }
            }
        }
    }
}

/** A scroll rod: a rounded bar a little wider than the paper, centred on [y]. */
private fun DrawScope.drawRod(color: Color, y: Float) {
    val thickness = 7.dp.toPx()
    val overhang = 10.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-overhang, y - thickness / 2),
        size = Size(size.width + overhang * 2, thickness),
        cornerRadius = CornerRadius(thickness / 2),
    )
}
