package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.FilledIcons
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun AlbumActionBottomSheet(
    show: Boolean,
    albumName: String,
    isCalculatingReplayGain: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCalculateReplayGain: () -> Unit
) {
    WindowBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SmallTitle(
                text = albumName.ifBlank { stringResource(R.string.album_detail_title) },
                insideMargin = PaddingValues(4.dp)
            )
            Card(
                modifier = Modifier.padding(bottom = 12.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryContainer
                )
            ) {
                ArrowPreference(
                    startAction = { PreferenceIcon(FilledIcons.Volume2, PreferenceIconTone.Library, enabled = !isCalculatingReplayGain) },
                    title = stringResource(R.string.menu_action_calculate_album_replay_gain),
                    enabled = !isCalculatingReplayGain,
                    onClick = {
                        if (!isCalculatingReplayGain) {
                            onCalculateReplayGain()
                        }
                    }
                )
                ArrowPreference(
                    startAction = { PreferenceIcon(FilledIcons.Share2, PreferenceIconTone.Tools) },
                    title = stringResource(R.string.menu_action_share_album),
                    onClick = onShare
                )
                ArrowPreference(
                    startAction = { PreferenceIcon(FilledIcons.Trash, PreferenceIconTone.Danger) },
                    title = stringResource(R.string.menu_action_delete_album),
                    summary = stringResource(R.string.menu_action_delete_album_sub),
                    titleColor = BasicComponentColors(
                        MiuixTheme.colorScheme.error,
                        MiuixTheme.colorScheme.disabledOnSecondaryVariant
                    ),
                    onClick = onDelete
                )
            }
        }
    }
}
