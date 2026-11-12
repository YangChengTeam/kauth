package com.yangfeisg.kauth.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangfeisg.kauth.core.designsystem.component.RollingCode
import com.yangfeisg.kauth.core.designsystem.component.drawBrushArc
import com.yangfeisg.kauth.core.designsystem.icon.KauthIcons
import com.yangfeisg.kauth.core.designsystem.theme.CodeTextStyle
import kotlin.random.Random

private const val DEMO_PERIOD_MILLIS = 4000

/** The first screen of a new install: a live demo of what the app does, the two ways in, and a restore. */
@Composable
internal fun EmptyState(
    onScanQr: () -> Unit,
    onEnterKey: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DemoCode()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onScanQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(painterResource(KauthIcons.Camera), contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.home_scan_qr))
        }
        OutlinedButton(
            onClick = onEnterKey,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(painterResource(KauthIcons.Keyboard), contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.home_enter_key))
        }
        TextButton(onClick = onImport) { Text(stringResource(R.string.home_restore_backup)) }
    }
}

/**
 * A sped-up picture of an account: a made-up code inside a brushed circle that runs down,
 * rolling over to a new code each time the brush lifts.
 */
@Composable
private fun DemoCode(modifier: Modifier = Modifier) {
    val remaining = remember { Animatable(1f) }
    var code by remember { mutableStateOf(randomCode()) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining.snapTo(1f)
            remaining.animateTo(0f, tween(DEMO_PERIOD_MILLIS, easing = LinearEasing))
            code = randomCode()
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface

    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val brushWidth = 14.dp.toPx()
            val radius = (size.minDimension - brushWidth) / 2
            drawCircle(ink.copy(alpha = 0.08f), radius, style = Stroke(1.dp.toPx()))
            drawBrushArc(ink, center, radius, startAngle = -90f, sweepAngle = 360f * remaining.value, maxWidth = brushWidth)
        }
        RollingCode(code = code, color = ink, style = CodeTextStyle.copy(fontSize = 30.sp))
    }
}

private fun randomCode() = Random.nextInt(1_000_000).toString().padStart(6, '0')
