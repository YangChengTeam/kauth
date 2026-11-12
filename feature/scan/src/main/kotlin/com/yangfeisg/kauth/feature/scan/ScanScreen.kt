package com.yangfeisg.kauth.feature.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yangfeisg.kauth.core.designsystem.icon.KauthIcons
import com.yangfeisg.kauth.core.designsystem.theme.Cinnabar

@Composable
internal fun ScanRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onEnterKey: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionRequest.launch(Manifest.permission.CAMERA)
    }
    // Coming back from Settings may have changed the answer.
    LifecycleResumeEffect(Unit) {
        hasCameraPermission = context.hasCameraPermission()
        onPauseOrDispose {}
    }

    // The camera screen is dark whatever the theme, like any viewfinder.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCameraPermission) {
            QrCameraPreview(
                onQrCode = { viewModel.onQrCode(it, onSaved) },
                modifier = Modifier.fillMaxSize(),
            )
            Viewfinder(Modifier.fillMaxSize())
            val progress = viewModel.migrationProgress
            Text(
                text = when {
                    viewModel.invalidCode -> stringResource(R.string.scan_invalid)
                    progress != null -> stringResource(R.string.scan_migration_progress, progress.scanned, progress.total)
                    else -> stringResource(R.string.scan_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
            )
        } else {
            PermissionNeeded(
                onOpenSettings = context::openAppSettings,
                onEnterKey = onEnterKey,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp),
        ) {
            Icon(painterResource(KauthIcons.ArrowBack), stringResource(R.string.scan_back), tint = Color.White)
        }
    }
}

/** Dims the picture except for a square window in the middle, which a light bar keeps sweeping. */
@Composable
private fun Viewfinder(modifier: Modifier = Modifier) {
    val sweep by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sweep",
    )
    // Offscreen so that BlendMode.Clear cuts through the scrim only, not the camera beneath.
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val side = size.minDimension * 0.7f
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
        val corner = CornerRadius(28.dp.toPx())
        drawRect(Color.Black.copy(alpha = 0.6f))
        drawRoundRect(Color.Black, topLeft, Size(side, side), corner, blendMode = BlendMode.Clear)
        drawRoundRect(Color.White.copy(alpha = 0.9f), topLeft, Size(side, side), corner, style = Stroke(2.dp.toPx()))

        val inset = 20.dp.toPx()
        val y = topLeft.y + inset + (side - inset * 2) * sweep
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Cinnabar, Cinnabar, Color.Transparent),
                startX = topLeft.x + inset,
                endX = topLeft.x + side - inset,
            ),
            start = Offset(topLeft.x + inset, y),
            end = Offset(topLeft.x + side - inset, y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PermissionNeeded(onOpenSettings: () -> Unit, onEnterKey: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.scan_permission_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.scan_permission_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.scan_open_settings)) }
        TextButton(onClick = onEnterKey) { Text(stringResource(R.string.scan_enter_key), color = Color.White) }
    }
}

private fun Context.hasCameraPermission() =
    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
