package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Shared layout for the illustrated empty states introduced by the poster pages. */
@Composable
fun IllustratedEmptyState(
    title: String,
    summary: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
    illustration: @Composable () -> Unit = { EmptyStateIllustration(compact = compact) },
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scrollModifier = if (constraints.hasBoundedHeight) {
            Modifier.verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
                .then(scrollModifier).padding(if (compact) 16.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            illustration()
            Spacer(Modifier.height(if (compact) 12.dp else 28.dp))
            Text(title, style = MiuixTheme.textStyles.title4, textAlign = TextAlign.Center)
            if (!summary.isNullOrBlank()) {
                Text(
                    summary,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (actionText != null) {
                Spacer(Modifier.height(20.dp))
                TextButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.widthIn(min = 180.dp).heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
