package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.yangfeisg.kauth.core.designsystem.theme.CodeTextStyle

private const val ROLL_MILLIS = 380
private const val STAGGER_MILLIS = 45

/**
 * A one-time code in two halves ("123 456") whose digits roll over like an odometer, one
 * after another from the left, whenever the code changes.
 */
@Composable
fun RollingCode(
    code: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = CodeTextStyle,
) {
    if (code.isEmpty()) {
        Text("–", modifier = modifier, style = style, color = color)
        return
    }
    val half = (code.length + 1) / 2
    Row(modifier.clipToBounds()) {
        code.forEachIndexed { index, digit ->
            if (index == half) Spacer(Modifier.width(10.dp))
            val delay = index * STAGGER_MILLIS
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    (slideInVertically(tween(ROLL_MILLIS, delay)) { it } + fadeIn(tween(ROLL_MILLIS, delay)))
                        .togetherWith(
                            slideOutVertically(tween(ROLL_MILLIS, delay)) { -it } + fadeOut(tween(ROLL_MILLIS, delay)),
                        )
                },
                label = "digit",
            ) { shown ->
                Text(shown.toString(), style = style, color = color)
            }
        }
    }
}
