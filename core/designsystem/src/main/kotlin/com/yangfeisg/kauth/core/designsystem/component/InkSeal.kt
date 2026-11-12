package com.yangfeisg.kauth.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A cinnabar seal, as stamped on a finished painting, carved with a single character. */
@Composable
fun InkSeal(character: String, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(size * 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = character,
            color = MaterialTheme.colorScheme.onTertiary,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.56f).sp,
            lineHeight = (size.value * 0.6f).sp,
        )
    }
}
