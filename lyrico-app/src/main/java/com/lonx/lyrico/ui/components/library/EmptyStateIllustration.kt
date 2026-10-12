package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Neutral list illustration for pages without a domain-specific illustration. */
@Composable
internal fun EmptyStateIllustration(compact: Boolean) {
    val colors = MiuixTheme.colorScheme
    val scale = if (compact) 0.45f else 1f
    val shape = RoundedCornerShape((16 * scale).dp)
    Box(Modifier.size((188 * scale).dp, (152 * scale).dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.offset(x = (-18 * scale).dp, y = (6 * scale).dp)
                .graphicsLayer { rotationZ = -10f }
                .size((112 * scale).dp, (132 * scale).dp)
                .background(colors.primary.copy(alpha = 0.08f), shape)
                .border(1.dp, colors.primary.copy(alpha = 0.12f), shape),
        )
        Column(
            Modifier.size((112 * scale).dp, (132 * scale).dp)
                .background(colors.surfaceContainer, shape)
                .border(1.dp, colors.primary.copy(alpha = 0.12f), shape)
                .padding((16 * scale).dp),
            verticalArrangement = Arrangement.spacedBy((14 * scale).dp),
        ) {
            repeat(4) { index ->
                Box(
                    Modifier.fillMaxWidth(if (index == 3) 0.65f else 1f)
                        .height((10 * scale).dp)
                        .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape((4 * scale).dp)),
                )
            }
        }
    }
}
