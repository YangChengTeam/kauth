package com.yangfeisg.kauth.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangfeisg.kauth.core.designsystem.component.CountdownRing
import com.yangfeisg.kauth.core.designsystem.component.RollingCode
import com.yangfeisg.kauth.core.designsystem.icon.KauthIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val COPIED_MILLIS = 1500L
private const val BLOOM_MILLIS = 800
private const val DISSOLVE_MILLIS = 1100
private const val WISPS = 6
// The seals, the gap between them, the row's own end margin, and a little room before them.
private val SEAL_SIZE = 34.dp
private val SEAL_GAP = 10.dp
private val ROW_END_PADDING = 24.dp
private val ACTIONS_WIDTH = ROW_END_PADDING + SEAL_SIZE * 2 + SEAL_GAP + 14.dp

/**
 * One account, written straight onto the page: its name, the current code, and the
 * countdown (or the next-code button), over a faint brushed rule. A tap copies the code;
 * ink blooms where the finger landed and the name briefly reads "Code copied".
 *
 * Swiped left, the row draws back from a wash of ink and two seals, edit and delete; let go past
 * halfway and it stays [open] on them. Once [dissolving], the row washes away like ink
 * dropped in water, and [onDissolved] is called when nothing is left of it.
 */
@Composable
internal fun AccountRow(
    item: AccountItem,
    onCopy: () -> Unit,
    onNextCode: () -> Unit,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dissolving: Boolean = false,
    onDissolved: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val ink = MaterialTheme.colorScheme.onSurface
    val accent by animateColorAsState(
        targetValue = if (item.expiringSoon) MaterialTheme.colorScheme.error else ink,
        label = "accent",
    )
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MILLIS)
            copied = false
        }
    }

    val dissolve = remember { Animatable(0f) }
    val currentOnDissolved by rememberUpdatedState(onDissolved)
    LaunchedEffect(dissolving) {
        if (dissolving) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            dissolve.animateTo(1f, tween(DISSOLVE_MILLIS, easing = FastOutSlowInEasing))
            currentOnDissolved()
        }
    }

    // How far the row sits to the left, in px: 0 closed, -openWidth showing the seals.
    val swipe = remember { Animatable(0f) }
    val openWidth = with(LocalDensity.current) { ACTIONS_WIDTH.toPx() }
    val settleSpec = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
    LaunchedEffect(open) { swipe.animateTo(if (open) -openWidth else 0f, settleSpec) }
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)
    val scope = rememberCoroutineScope()
    // Root y of the row's top and of the middle of its code line, so the seals sit level with the code.
    var rowTop by remember { mutableFloatStateOf(0f) }
    var codeLineCenter by remember { mutableFloatStateOf(Float.NaN) }
    val editLabel = stringResource(R.string.home_edit)
    val deleteLabel = stringResource(R.string.home_delete)

    // In place of a ripple: a wash of ink spreading from the touch and soaking away.
    val interactionSource = remember { MutableInteractionSource() }
    val bloom = remember { Animatable(1f) }
    var bloomOrigin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                bloomOrigin = interaction.pressPosition
                launch {
                    bloom.snapTo(0f)
                    bloom.animateTo(1f, tween(BLOOM_MILLIS))
                }
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowTop = it.positionInRoot().y }
            .pointerInput(dissolving, openWidth) {
                if (dissolving) return@pointerInput
                var dragX = 0f
                var pastHalf = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragX = swipe.value
                        pastHalf = dragX < -openWidth / 2
                    },
                    onDragEnd = {
                        val target = if (pastHalf) -openWidth else 0f
                        currentOnOpenChange(pastHalf)
                        scope.launch { swipe.animateTo(target, settleSpec) }
                    },
                    onDragCancel = {
                        scope.launch { swipe.animateTo(if (open) -openWidth else 0f, settleSpec) }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    // Past the seals the paper resists, as if held by the ink.
                    val resisted = if (dragX < -openWidth) dragAmount * 0.3f else dragAmount
                    dragX = (dragX + resisted).coerceIn(-size.width * 0.7f, 0f)
                    scope.launch { swipe.snapTo(dragX) }
                    val nowPastHalf = dragX < -openWidth / 2
                    if (nowPastHalf && !pastHalf) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    pastHalf = nowPastHalf
                }
            }
            // The same actions without a swipe, for screen readers.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(editLabel) {
                        onEdit()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
    ) {
        SwipeActions(
            progress = { -swipe.value / openWidth },
            codeLineCenter = { codeLineCenter - rowTop },
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = Modifier.matchParentSize(),
        )
        AccountRowContent(
            item = item,
            copied = copied,
            accent = accent,
            ink = ink,
            dissolve = { dissolve.value },
            bloom = { bloom.value },
            bloomOrigin = { bloomOrigin },
            interactionSource = interactionSource,
            enabled = !dissolving,
            onClick = {
                // An open row closes on a tap rather than copying.
                if (open) {
                    onOpenChange(false)
                } else {
                    onCopy()
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    copied = true
                }
            },
            onNextCode = onNextCode,
            onCodeLinePositioned = { codeLineCenter = it },
            // The paper's right edge draws back over the seals; the code itself stays put.
            modifier = Modifier.layout { measurable, constraints ->
                val width = (constraints.maxWidth + swipe.value.roundToInt()).coerceAtLeast(0)
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) { placeable.place(0, 0) }
            },
        )
    }
}

/** The row's own writing, laid on the paper that slides over [SwipeActions]. */
@Composable
private fun AccountRowContent(
    item: AccountItem,
    copied: Boolean,
    accent: Color,
    ink: Color,
    dissolve: () -> Float,
    bloom: () -> Float,
    bloomOrigin: () -> Offset,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onClick: () -> Unit,
    onNextCode: () -> Unit,
    onCodeLinePositioned: (centerInRoot: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            // Outside the clip: the ink may spill over the neighbouring rows as it spreads.
            .drawWithContent {
                drawContent()
                drawDissolve(dissolve(), ink)
            }
            .clipToBounds()
            .graphicsLayer {
                // The writing loosens and lets go as the water takes it.
                val d = dissolve()
                alpha = 1f - d * d
                scaleX = 1f + 0.04f * d
                scaleY = scaleX
            }
            .blur((dissolve() * 14).dp, BlurredEdgeTreatment.Unbounded)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                val spread = bloom()
                if (spread < 1f) {
                    val radius = size.width * 0.7f * spread
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to ink.copy(alpha = 0.16f * (1f - spread)),
                            0.7f to ink.copy(alpha = 0.10f * (1f - spread)),
                            1f to Color.Transparent,
                            center = bloomOrigin(),
                            radius = radius.coerceAtLeast(1f),
                        ),
                        radius = radius,
                        center = bloomOrigin(),
                    )
                }
                // The rule under the row: a stroke that fades out at both ends.
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.12f to ink.copy(alpha = 0.16f),
                        0.7f to ink.copy(alpha = 0.10f),
                        1f to Color.Transparent,
                    ),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            .padding(start = 28.dp, top = 18.dp, end = ROW_END_PADDING, bottom = 16.dp),
    ) {
        AnimatedContent(
            targetState = copied,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "title",
        ) { showCopied ->
            Text(
                text = if (showCopied) stringResource(R.string.home_code_copied) else item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (showCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.onGloballyPositioned {
                onCodeLinePositioned(it.positionInRoot().y + it.size.height / 2f)
            },
        ) {
            RollingCode(code = item.code, color = accent, modifier = Modifier.weight(1f))
            if (item.isTimeBased) {
                CountdownRing(periodSeconds = item.period, color = accent)
            } else {
                IconButton(onClick = onNextCode) {
                    Icon(
                        painter = painterResource(KauthIcons.Refresh),
                        contentDescription = stringResource(R.string.home_next_code),
                    )
                }
            }
        }
    }
}

/**
 * What a left swipe uncovers: ink washing in from the right edge, darker the further the row
 * goes, and two seals stamped one after the other as [progress] runs to 1 (fully open):
 * delete in cinnabar nearest the edge, edit in ink beside it.
 */
@Composable
private fun SwipeActions(
    progress: () -> Float,
    codeLineCenter: () -> Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Box(
        modifier.drawBehind {
            val amount = progress().coerceIn(0f, 1.4f)
            if (amount > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to ink.copy(alpha = 0.05f * amount),
                        1f to ink.copy(alpha = 0.18f * amount),
                    ),
                )
            }
        },
    ) {
        Row(
            // Right edge on the row's end margin, middle level with the code line.
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val center = codeLineCenter().takeUnless { it.isNaN() } ?: (constraints.maxHeight / 2f)
                        placeable.place(
                            x = constraints.maxWidth - placeable.width,
                            y = (center - placeable.height / 2f).roundToInt(),
                        )
                    }
                }
                .padding(end = ROW_END_PADDING),
            horizontalArrangement = Arrangement.spacedBy(SEAL_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionSeal(
                icon = KauthIcons.Edit,
                label = stringResource(R.string.home_edit),
                color = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                shown = { ((progress() - 0.3f) / 0.7f).coerceIn(0f, 1f) },
                onClick = onEdit,
            )
            ActionSeal(
                icon = KauthIcons.Delete,
                label = stringResource(R.string.home_delete),
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shown = { (progress() / 0.7f).coerceIn(0f, 1f) },
                onClick = onDelete,
            )
        }
    }
}

/** A square seal carrying an action's icon; it turns into place and grows as [shown] goes 0..1. */
@Composable
private fun ActionSeal(
    icon: Int,
    label: String,
    color: Color,
    contentColor: Color,
    shown: () -> Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SEAL_SIZE)
            .graphicsLayer {
                val amount = shown()
                alpha = amount
                scaleX = 0.4f + 0.6f * amount
                scaleY = scaleX
                rotationZ = -18f * (1f - amount)
            }
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Ink dropped in water, [progress] 0..1: a wash spreads from the middle of the row, darkest
 * halfway, while wisps drift up and away from it, and all of it clears by the end.
 */
private fun DrawScope.drawDissolve(progress: Float, ink: Color) {
    if (progress <= 0f || progress >= 1f) return
    val strength = sin(progress * PI.toFloat())
    val origin = Offset(size.width * 0.32f, size.height * 0.55f)
    val radius = size.width * (0.12f + 0.45f * progress)
    drawCircle(
        brush = Brush.radialGradient(
            0f to ink.copy(alpha = 0.26f * strength),
            0.5f to ink.copy(alpha = 0.12f * strength),
            1f to Color.Transparent,
            center = origin,
            radius = radius,
        ),
        radius = radius,
        center = origin,
    )
    for (i in 0 until WISPS) {
        val spreadX = (i - (WISPS - 1) / 2f) / WISPS
        val wisp = origin + Offset(
            x = size.width * spreadX * (0.3f + 0.9f * progress),
            y = -size.height * (0.15f + 0.9f * progress) * (0.6f + 0.4f * ((i * 3) % WISPS) / WISPS),
        )
        val wispRadius = (22 + (i * 7) % 18).dp.toPx() * (0.6f + progress)
        drawCircle(
            brush = Brush.radialGradient(
                0f to ink.copy(alpha = 0.14f * strength),
                1f to Color.Transparent,
                center = wisp,
                radius = wispRadius,
            ),
            radius = wispRadius,
            center = wisp,
        )
    }
}

/**
 * Lets the rows rise into place one after another when the list first appears. With
 * [enabled] false (rows scrolled in later) the modifier does nothing.
 */
@Composable
internal fun Modifier.staggeredEntrance(index: Int, enabled: Boolean): Modifier {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index.coerceAtMost(8) * 70L)
            progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 40.dp.toPx()
    }
}
