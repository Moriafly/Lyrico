package com.lonx.lyrico.ui.components.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.data.model.UiEngine
import com.lonx.lyrico.ui.components.salt.SaltBottomSheet
import com.lonx.lyrico.ui.theme.LocalUiEngine
import com.lonx.lyrico.ui.components.miuix.MiuixBottomSheet

/** Single native window, with the existing Miuix content retained by the caller. */
@Composable
internal fun EditorSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    enableNestedScroll: Boolean = false,
    tools: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiEngine.current == UiEngine.SALT)
        SaltBottomSheet(
            show = show,
            onDismissRequest = onDismissRequest,
            title = title,
            header = tools?.let { actions ->
                {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) { actions() }
                }
            },
            content = content,
        )
    else
        MiuixBottomSheet(
            show = show,
            title = title,
            enableNestedScroll = enableNestedScroll,
            onDismissRequest = onDismissRequest,
            tools = tools,
            content = content,
        )
}
