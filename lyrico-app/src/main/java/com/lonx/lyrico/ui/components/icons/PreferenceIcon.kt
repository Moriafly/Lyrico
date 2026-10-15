package com.lonx.lyrico.ui.components.icons

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class PreferenceIconTone {
    Appearance, Library, Lyrics, Tools, Danger;

    @Composable
    fun color(): Color {
        val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        return when (this) {
            Appearance -> if (dark) Color(0xFFC4A7FF) else Color(0xFF7952B3)
            Library -> if (dark) Color(0xFFF3BF75) else Color(0xFF99600C)
            Lyrics -> if (dark) Color(0xFF7DD6AB) else Color(0xFF23784F)
            Tools -> MiuixTheme.colorScheme.primary
            Danger -> MiuixTheme.colorScheme.error
        }
    }
}

/** Decorative leading icon; the preference title supplies its accessible name. */
@Composable
internal fun PreferenceIcon(
    imageVector: ImageVector,
    tone: PreferenceIconTone = PreferenceIconTone.Tools,
    modifier: Modifier = Modifier.padding(horizontal = 8.dp),
    enabled: Boolean = true,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(24.dp),
        tint = tone.color().let { if (enabled) it else it.copy(alpha = 0.38f) },
    )
}
