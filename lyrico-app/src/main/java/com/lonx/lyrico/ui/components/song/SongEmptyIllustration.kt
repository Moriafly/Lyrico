package com.lonx.lyrico.ui.components.song

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SongEmptyIllustration() {
    val colors = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.size(width = 188.dp, height = 152.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.offset(x = (-18).dp, y = 6.dp).graphicsLayer { rotationZ = -10f }
                .size(width = 112.dp, height = 132.dp)
                .background(colors.primary.copy(alpha = 0.08f), shape)
                .border(1.dp, colors.primary.copy(alpha = 0.12f), shape),
        )
        Column(
            Modifier.size(width = 112.dp, height = 132.dp).shadow(4.dp, shape)
                .background(colors.surfaceContainer, shape)
                .border(1.dp, colors.primary.copy(alpha = 0.12f), shape).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().height(62.dp)
                    .background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MiuixIcons.Music, null, tint = colors.primary, modifier = Modifier.size(36.dp))
            }
            Box(Modifier.fillMaxWidth().height(5.dp).background(colors.primary.copy(alpha = 0.2f), shape))
            Box(Modifier.fillMaxWidth(0.65f).height(5.dp).background(colors.primary.copy(alpha = 0.12f), shape))
        }
    }
}
