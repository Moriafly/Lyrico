package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.FilledIcons
import com.lonx.lyrico.data.model.ExportDestination
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ExportDestinationBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (ExportDestination) -> Unit
) {
    var destination by remember(show) {
        mutableStateOf(ExportDestination.SELECTED_DIRECTORY)
    }

    YesNoBottomSheet(
        show = show,
        title = stringResource(R.string.export_destination_title),
        onDismissRequest = onDismissRequest,
        onConfirm = { onConfirm(destination) },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryContainer
                    )
                ) {
                    RadioButtonPreference(
                        startAction = { PreferenceIcon(FilledIcons.Folder, PreferenceIconTone.Library) },
                        title = stringResource(R.string.export_destination_selected_folder),
                        summary = stringResource(R.string.export_destination_selected_folder_summary),
                        selected = destination == ExportDestination.SELECTED_DIRECTORY,
                        onClick = { destination = ExportDestination.SELECTED_DIRECTORY }
                    )
                    RadioButtonPreference(
                        startAction = { PreferenceIcon(FilledIcons.FileMusic, PreferenceIconTone.Library) },
                        title = stringResource(R.string.export_destination_audio_folder),
                        summary = stringResource(R.string.export_destination_audio_folder_summary),
                        selected = destination == ExportDestination.AUDIO_DIRECTORY,
                        onClick = { destination = ExportDestination.AUDIO_DIRECTORY }
                    )
                }
            }
        }
    )
}
