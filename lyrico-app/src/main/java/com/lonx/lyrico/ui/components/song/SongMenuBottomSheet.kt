package com.lonx.lyrico.ui.components.song

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.entity.SongEntity
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SongMenuBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    enableNestedScroll: Boolean = true,
    song: SongEntity,
    onPlay: () -> Unit,
    showInfo: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
) {
    val songTitle = song.title.takeIf { !it.isNullOrBlank() } ?: song.fileName
    val text =
        song.artist.takeIf { !it.isNullOrBlank() }?.let { "$songTitle - $it" } ?: songTitle
    com.lonx.lyrico.ui.components.base.ItemMenuBottomSheet(
        show = show, label = text, onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished, enableNestedScroll = enableNestedScroll
    ) {
    ArrowPreference(
        title = stringResource(R.string.menu_action_play),
        summary = stringResource(R.string.menu_action_play_sub),
        onClick = { onPlay() }
    )
    ArrowPreference(
        title = stringResource(R.string.menu_action_info),
        onClick = { showInfo() }
    )
    ArrowPreference(
        title = stringResource(R.string.menu_action_share),
        onClick = { onShare() }
    )
    ArrowPreference(
        title = stringResource(R.string.menu_action_rename),
        onClick = { onRename() }
    )
    ArrowPreference(
        title = stringResource(R.string.menu_action_delete),
        summary = stringResource(R.string.menu_action_delete_sub),
        titleColor = BasicComponentColors(
            MiuixTheme.colorScheme.error,
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        ),
        onClick = { onDelete() }
    )
    }
}
