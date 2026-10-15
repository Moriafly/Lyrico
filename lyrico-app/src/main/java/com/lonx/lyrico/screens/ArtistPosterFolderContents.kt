package com.lonx.lyrico.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.FilledIcons
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.viewmodel.ArtistPosterFile
import com.lonx.lyrico.viewmodel.ArtistPosterFolder
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun ArtistPosterFolderContents(
    folder: ArtistPosterFolder,
    revision: Long,
    isLoading: Boolean,
    enabled: Boolean,
    hasError: Boolean,
    onChangeFolder: () -> Unit,
    onRefresh: () -> Unit,
    onFileMenu: (ArtistPosterFile) -> Unit,
    onCrop: (ArtistPosterFile, android.graphics.Bitmap) -> Unit,
    onRename: (ArtistPosterFile) -> Unit,
    onArtistDetails: (ArtistPosterFile) -> Unit,
    onPreviewSelection: (String) -> Unit,
    previewUri: String?,
    onDismissPreview: () -> Unit,
    padding: PaddingValues,
    scrollBehavior: ScrollBehavior
) {
    val files = folder.posters.orEmpty()
    if (files.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            ArtistPosterDirectoryCard(folder, enabled && !isLoading, onChangeFolder,
                Modifier.padding(top = 12.dp))
            if (isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val unavailable = folder.posters == null
                IllustratedEmptyState(
                    kind = EmptyStateKind.Cover,
                    title = stringResource(if (unavailable) R.string.artist_poster_folder_unavailable else R.string.artist_poster_no_files),
                    summary = stringResource(if (hasError) R.string.artist_poster_folder_error else if (unavailable) R.string.artist_poster_folders_empty else R.string.artist_poster_empty_hint),
                    icon = if (unavailable) FilledIcons.Folder else FilledIcons.RefreshCw,
                    actionText = stringResource(if (unavailable) R.string.artist_poster_folder_change else R.string.action_refresh_folder),
                    onAction = if (unavailable) onChangeFolder else onRefresh,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = padding,
        modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        overscrollEffect = null
    ) {
        item(key = "folder", span = { GridItemSpan(maxLineSpan) }) {
            ArtistPosterDirectoryCard(folder, enabled && !isLoading, onChangeFolder,
                Modifier.padding(top = 12.dp))
        }
        item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
            SmallTitle(
                text = stringResource(R.string.artist_poster_file_count, files.size),
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(12.dp))
            }
        }
        if (hasError) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.artist_poster_folder_error),
                    color = MiuixTheme.colorScheme.error,
                    fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(files, key = { it.uri }) { file ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = if (enabled && !isLoading) { { onFileMenu(file) } } else null,
                showIndication = true
            ) {
                PosterImage(file, revision, Modifier.fillMaxWidth().aspectRatio(4f / 5f), ContentScale.Crop)
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        file.artistName ?: stringResource(R.string.label_artist_image_unmatched),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        file.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
    val previewIndex = files.indexOfFirst { it.uri == previewUri }
    if (previewIndex >= 0) {
        PosterPreview(files, previewIndex, revision,
            onRename = onRename, onArtistDetails = onArtistDetails, onPreviewSelection = onPreviewSelection,
            onCrop = onCrop, enabled = enabled,
            onDismiss = onDismissPreview)
    }
}

@Composable
private fun ArtistPosterDirectoryCard(
    folder: ArtistPosterFolder,
    enabled: Boolean,
    onChangeFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = if (enabled) onChangeFolder else null,
        showIndication = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).background(
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)
                ), contentAlignment = Alignment.Center
            ) {
                Icon(FilledIcons.FolderPlus, null, tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(folder.name, style = MiuixTheme.textStyles.body1,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(folder.path, style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Text(stringResource(R.string.artist_poster_folder_change),
                style = MiuixTheme.textStyles.footnote1,
                color = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun PosterImage(file: ArtistPosterFile, revision: Long, modifier: Modifier, scale: ContentScale) {
    val context = LocalContext.current
    val request = remember(file, revision, context) {
        ImageRequest.Builder(context)
            .data(if (file.name.endsWith(".mp4", true)) file else file.uri.toUri())
            .memoryCacheKey("poster:${file.uri}:${file.lastModified}:$revision")
            .diskCacheKey("poster:${file.uri}:${file.lastModified}:$revision")
            .build()
    }
    var failed by remember(file, revision) { mutableStateOf(false) }
    var loading by remember(file, revision) { mutableStateOf(true) }
    Box(modifier.background(MiuixTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        AsyncImage(request, file.name, Modifier.fillMaxSize(), contentScale = scale,
            onSuccess = { loading = false; failed = false }, onError = { loading = false; failed = true })
        if (loading) CircularProgressIndicator(size = 24.dp)
        if (failed) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(FilledIcons.Image, null)
            Text(stringResource(R.string.artist_poster_preview_error), fontSize = MiuixTheme.textStyles.body2.fontSize)
        }
    }
}

@Composable
private fun PosterPreview(files: List<ArtistPosterFile>, initialPage: Int, revision: Long,
    onRename: (ArtistPosterFile) -> Unit, onArtistDetails: (ArtistPosterFile) -> Unit,
    onPreviewSelection: (String) -> Unit, onCrop: (ArtistPosterFile, android.graphics.Bitmap) -> Unit,
    enabled: Boolean, onDismiss: () -> Unit) {
    val renameLabel = stringResource(R.string.action_rename)
    val detailsLabel = stringResource(R.string.poster_artist_details)
    val unmatchedLabel = stringResource(R.string.poster_artist_unmatched)
    com.lonx.lyrico.ui.components.poster.ArtistPosterPreview(
        items = remember(files, revision) { files.map { file ->
            com.lonx.lyrico.ui.components.poster.PosterPreviewMedia(file.name, file.uri,
                isVideo = file.name.endsWith(".mp4", true), revision = revision + file.lastModified)
        } }, initialPage = initialPage,
        editingEnabled = enabled,
        onCrop = { index, bitmap -> onCrop(files[index], bitmap) },
        menuEntry = { index ->
            val file = files[index]
            DropdownEntry(items = listOf(
                DropdownItem(
                    icon = { modifier -> PreferenceIcon(FilledIcons.Pencil, PreferenceIconTone.Tools, modifier = modifier, enabled = enabled) },
                    text = renameLabel, enabled = enabled, onClick = { onRename(file) }),
                DropdownItem(
                    icon = { modifier -> PreferenceIcon(FilledIcons.Users, PreferenceIconTone.Library, modifier = modifier, enabled = enabled && file.artistId != null) },
                    text = detailsLabel, enabled = enabled && file.artistId != null,
                    summary = if (file.artistId == null) unmatchedLabel else null,
                    onClick = { onArtistDetails(file) })
            ))
        },
        onCurrentChanged = { onPreviewSelection(files[it].uri) }, onDismiss = onDismiss
    )
}
