package com.lonx.lyrico.screens

import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Folder
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
import androidx.compose.ui.graphics.Color
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
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.viewmodel.ArtistPosterFile
import com.lonx.lyrico.viewmodel.ArtistPosterFolder
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddFolder
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun ArtistPosterFolderContents(
    folder: ArtistPosterFolder,
    revision: Long,
    isLoading: Boolean,
    hasError: Boolean,
    onChangeFolder: () -> Unit,
    onRefresh: () -> Unit,
    onFileMenu: (ArtistPosterFile) -> Unit,
    padding: PaddingValues,
    scrollBehavior: ScrollBehavior
) {
    val files = folder.posters.orEmpty()
    var previewUri by remember(folder.uri) { mutableStateOf<String?>(null) }
    if (files.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            ArtistPosterDirectoryCard(folder, !isLoading, onChangeFolder,
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
                    icon = if (unavailable) MiuixIcons.Folder else MiuixIcons.Refresh,
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
            ArtistPosterDirectoryCard(folder, !isLoading, onChangeFolder,
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
                onClick = { previewUri = file.uri },
                showIndication = true
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    PosterImage(file, revision, Modifier.fillMaxSize(), ContentScale.Crop)
                    if (file.name.endsWith(".mp4", true)) {
                        Text("MP4", color = Color.White, modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(6.dp).background(Color.Black.copy(alpha = .65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        file.name,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                    )
                    IconButton(onClick = { onFileMenu(file) }, enabled = !isLoading) {
                        Icon(MiuixIcons.More, stringResource(R.string.cd_more_actions))
                    }
                }
            }
        }
    }
    val previewIndex = files.indexOfFirst { it.uri == previewUri }
    if (previewIndex >= 0) {
        PosterPreview(files, previewIndex, revision,
            onFileMenu = { file -> previewUri = null; onFileMenu(file) },
            onDismiss = { previewUri = null })
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
                Icon(MiuixIcons.AddFolder, null, tint = MiuixTheme.colorScheme.primary,
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
            Icon(MiuixIcons.Image, null)
            Text(stringResource(R.string.artist_poster_preview_error), fontSize = MiuixTheme.textStyles.body2.fontSize)
        }
    }
}

@Composable
private fun PosterPreview(files: List<ArtistPosterFile>, initialPage: Int, revision: Long, onFileMenu: (ArtistPosterFile) -> Unit, onDismiss: () -> Unit) {
    com.lonx.lyrico.ui.components.poster.ArtistPosterPreview(
        pageCount = files.size, initialPage = initialPage, title = { files[it].name }, pageKey = { files[it].uri },
        videoUri = { files[it].takeIf { file -> file.name.endsWith(".mp4", true) }?.uri },
        onMore = { onFileMenu(files[it]) }, onDismiss = onDismiss,
        imageContent = { index, modifier -> PosterImage(files[index], revision, modifier, ContentScale.Fit) }
    )
}
