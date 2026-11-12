package com.yangfeisg.kauth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yangfeisg.kauth.core.designsystem.component.InkBloom
import com.yangfeisg.kauth.core.designsystem.component.InkLandscape
import com.yangfeisg.kauth.core.designsystem.component.InkSeal
import kotlinx.coroutines.delay

private const val EXIT_MILLIS = 1000
private const val EXIT_HOLD_MILLIS = 350L

/**
 * The whole app is one sheet of paper: the landscape is painted once, here, and the
 * screens are see-through layers over it, so the scene holds still while they change.
 */
@Composable
fun KauthApp(onExit: () -> Unit) {
    // Saveable, so the launch scene plays on a cold start only, not on every rotation.
    var launching by rememberSaveable { mutableStateOf(true) }
    // Leaving mirrors arriving: ink floods the page from its centre, the seal signs off,
    // and only then does the app close.
    var exiting by remember { mutableStateOf(false) }
    val exitInk = remember { Animatable(0f) }
    val currentOnExit by rememberUpdatedState(onExit)
    LaunchedEffect(exiting) {
        if (exiting) {
            exitInk.animateTo(1f, tween(EXIT_MILLIS, easing = FastOutSlowInEasing))
            delay(EXIT_HOLD_MILLIS)
            currentOnExit()
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        InkLandscape(Modifier.fillMaxSize())
        KauthNavHost(onExit = { exiting = true })
        // The launch scene ends on this same landscape, then dissolves to show the app beneath.
        AnimatedVisibility(visible = launching, enter = EnterTransition.None, exit = fadeOut(tween(700))) {
            LaunchScreen(onFinished = { launching = false })
        }
        if (exiting) {
            InkBloom(
                spread = { exitInk.value },
                // Also swallows touches, so nothing underneath reacts while the app leaves.
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {},
                reach = 1f,
                opacity = 1f,
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                InkSeal(
                    character = stringResource(R.string.launch_seal),
                    size = 56.dp,
                    modifier = Modifier.graphicsLayer {
                        val pressed = ((exitInk.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        alpha = pressed
                        scaleX = 1.6f - 0.6f * pressed
                        scaleY = scaleX
                    },
                )
            }
        }
    }
}
