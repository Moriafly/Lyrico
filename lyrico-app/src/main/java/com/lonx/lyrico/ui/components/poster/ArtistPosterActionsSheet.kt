package com.lonx.lyrico.ui.components.poster

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lonx.lyrico.R
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Artist poster actions; local images share one entry and a separate destination choice. */
@Composable
fun ArtistPosterActionsSheet(
    show: Boolean,
    onAddLocalPoster: () -> Unit,
    onViewPoster: () -> Unit,
    canViewPoster: Boolean,
    posterLoading: Boolean = false,
    posterError: Boolean = false,
    onDismissRequest: () -> Unit,
    onRemoveFromSongs: () -> Unit,
    onExportEmbedded: () -> Unit,
    enabled: Boolean = true
) {
    com.lonx.lyrico.ui.components.base.ItemMenuBottomSheet(
        show = show, label = stringResource(R.string.label_artist_image_options),
        enableNestedScroll = false, onDismissRequest = onDismissRequest
    ) {
        ArrowPreference(
            title = stringResource(R.string.poster_view_artwork),
            summary = if (posterError) stringResource(R.string.artist_poster_read_error)
                else if (!posterLoading && !canViewPoster) stringResource(R.string.artist_poster_missing_title) else null,
            enabled = enabled && canViewPoster,
            onClick = onViewPoster
        )
        if (posterLoading) top.yukonga.miuix.kmp.basic.CircularProgressIndicator()
        ArrowPreference(
            title = stringResource(R.string.label_change_artist_image),
            summary = stringResource(R.string.artist_poster_add_local_summary),
            enabled = enabled,
            onClick = onAddLocalPoster
        )
        ArrowPreference(
            title = stringResource(R.string.poster_remove_artwork),
            summary = stringResource(R.string.artist_poster_remove_summary),
            titleColor = BasicComponentColors(MiuixTheme.colorScheme.error, MiuixTheme.colorScheme.disabledOnSecondaryVariant),
            enabled = enabled,
            onClick = onRemoveFromSongs
        )
        ArrowPreference(
            title = stringResource(R.string.poster_export_embedded),
            summary = stringResource(R.string.artist_poster_export_summary),
            enabled = enabled,
            onClick = onExportEmbedded
        )
    }
}

@Composable
fun ArtistPosterDestinationPreferences(
    hasPosterFolder: Boolean,
    enabled: Boolean,
    onEmbedToSongs: () -> Unit,
    onSaveToFolder: () -> Unit
) {
    ArrowPreference(
        title = stringResource(R.string.label_artist_poster_embed_to_songs),
        summary = stringResource(R.string.artist_poster_embed_summary),
        enabled = enabled,
        onClick = onEmbedToSongs
    )
    ArrowPreference(
        title = stringResource(R.string.label_artist_poster_add_to_folder),
        enabled = enabled,
        summary = stringResource(if (hasPosterFolder) R.string.artist_poster_save_folder_summary else R.string.artist_poster_select_folder_summary),
        onClick = onSaveToFolder
    )
}
