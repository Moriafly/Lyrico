package com.lonx.lyrico.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.ui.components.library.SelectableLibraryItem
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.ui.components.bar.SongBatchSelectionActions
import com.lonx.lyrico.ui.components.bar.SongSelectionTopAppBar
import com.lonx.lyrico.ui.components.base.YesNoDialog
import com.lonx.lyrico.ui.components.cover.CoverImage
import com.lonx.lyrico.ui.components.library.AlbumReplayGainProgressBottomSheet
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.ui.components.song.SongActionSheets
import com.lonx.lyrico.ui.components.song.SongListItemContent
import com.lonx.lyrico.ui.components.song.SongListItemActions
import com.lonx.lyrico.viewmodel.AlbumActionsViewModel
import com.lonx.lyrico.viewmodel.AlbumDetailViewModel
import com.lonx.lyrico.viewmodel.SongSelectionViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.EditMetadataDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
@Destination<RootGraph>(route = "album_detail")
fun AlbumDetailScreen(
    navigator: DestinationsNavigator,
    albumId: Long
) {
    val viewModel: AlbumDetailViewModel = koinViewModel(
        parameters = { parametersOf(albumId) }
    )
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    val albumActionsViewModel: AlbumActionsViewModel = koinViewModel()
    val album by viewModel.album.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albumActionsUiState by albumActionsViewModel.uiState.collectAsStateWithLifecycle()
    val albumName = album?.name.orEmpty()
    val albumArtist = album?.albumArtist?.takeIf { it.isNotBlank() }
        ?: songs.firstNotNullOfOrNull { song ->
            song.albumArtist?.trim()?.takeIf { it.isNotBlank() }
        }
    val isSelectionMode by selectionViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongUris by selectionViewModel.selectedSongUris.collectAsStateWithLifecycle()
    val swipeAnchorUri by selectionViewModel.swipeAnchorUri.collectAsStateWithLifecycle()
    val swipeSelectionLabel = stringResource(
        if (!isSelectionMode) {
            R.string.swipe_selection_enter_selection
        } else if (swipeAnchorUri == null) {
            R.string.swipe_selection_range_start
        } else {
            R.string.swipe_selection_range_end
        }
    )
    val swipeSelectionSecondaryLabel = if (!isSelectionMode) {
        stringResource(R.string.swipe_selection_range_start)
    } else {
        null
    }
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteAlbumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(albumActionsViewModel, context) {
        albumActionsViewModel.events.collect { message ->
            message.asString(context)?.let { text ->
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        if (isFabMenuExpanded) {
            isFabMenuExpanded = false
        } else {
            selectionViewModel.exitSelectionMode()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = isSelectionMode,
                    label = "AlbumDetailTopBarAnimation",
                    transitionSpec = {
                        val animationDuration = 300
                        val enter = fadeIn(tween(animationDuration)) +
                                slideInVertically(
                                    animationSpec = tween(
                                        animationDuration,
                                        easing = FastOutSlowInEasing
                                    ),
                                    initialOffsetY = { -it / 3 }
                                )
                        val exit = fadeOut(tween(animationDuration)) +
                                slideOutVertically(
                                    animationSpec = tween(
                                        animationDuration,
                                        easing = FastOutSlowInEasing
                                    ),
                                    targetOffsetY = { -it / 3 }
                                )

                        (enter togetherWith exit).using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { selectionMode ->
                    if (selectionMode) {
                        SongSelectionTopAppBar(
                            songs = songs,
                            selectedSongUris = selectedSongUris,
                            scrollBehavior = topAppBarScrollBehavior,
                            onSelectAll = selectionViewModel::selectAll,
                            onDeselectAll = selectionViewModel::deselectAll,
                            onClose = selectionViewModel::exitSelectionMode
                        )
                    } else {
                        SmallTopAppBar(
                            title = albumName.ifBlank { stringResource(R.string.album_detail_title) },
                            navigationIcon = {
                                IconButton(onClick = { navigator.popBackStack() }) {
                                    Icon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                OverlayIconDropdownMenu(
                                    entries = listOf(
                                        albumActionsDropdownEntry(
                                            isCalculatingReplayGain = albumActionsUiState.isCalculatingAlbumReplayGain,
                                            onShare = {
                                                albumActionsViewModel.shareAlbum(context, songs)
                                            },
                                            onDelete = {
                                                showDeleteAlbumDialog = true
                                            },
                                            onCalculateReplayGain = {
                                                albumActionsViewModel.calculateAlbumReplayGain(songs)
                                            }
                                        )
                                    )
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.More,
                                        contentDescription = stringResource(R.string.album_action_more)
                                    )
                                }
                            },
                            scrollBehavior = topAppBarScrollBehavior
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldTopHorizontalPadding(paddingValues))
                ) {
                    AlbumDetailHeader(
                        album = albumName,
                        albumArtist = albumArtist,
                        songCount = songs.size,
                        coverSong = songs.firstOrNull()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .scrollEndHaptic()
                            .overScrollVertical()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                            .weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 12.dp),
                        overscrollEffect = null
                    ) {
                        if (songs.isEmpty()) {
                            item("empty") {
                                IllustratedEmptyState(
                                    kind = EmptyStateKind.Songs,
                                    title = stringResource(R.string.empty_songs_title),
                                    compact = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        items(
                            items = songs,
                            key = { song ->
                                song.uri.takeIf { it.isNotBlank() && it != "0" } ?: "song-${song.id}"
                            }
                        ) { song ->
                            SelectableLibraryItem(
                                itemKey = song.uri,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedSongUris.contains(song.uri),
                                swipeSelectionLabel = swipeSelectionLabel,
                                swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                                onClick = {
                                    navigator.navigate(EditMetadataDestination(songFileUri = song.uri))
                                },
                                onToggleSelection = {
                                    selectionViewModel.toggleSelection(song.uri)
                                },
                                onSwipeSelection = {
                                    selectionViewModel.swipeSelect(song, songs)
                                }
                            ) {
                                SongListItemContent(
                                    song = song,
                                    showTrackNumbers = true,
                                    trailingContent = {
                                        Box(modifier = Modifier.padding(end = 8.dp)) {
                                            SongListItemActions(
                                                isSelectionMode = isSelectionMode,
                                                isSelected = selectedSongUris.contains(song.uri),
                                                onToggleSelection = {
                                                    selectionViewModel.toggleSelection(song.uri)
                                                },
                                                onShowMenu = {
                                                    selectedSong = song
                                                    showMenuSheet = true
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                SongActionSheets(
                    selectedSong = selectedSong,
                    showMenuSheet = showMenuSheet,
                    showDetailSheet = showDetailSheet,
                    showDeleteDialog = showDeleteDialog,
                    showRenameDialog = showRenameDialog,
                    onDismissMenu = { showMenuSheet = false },
                    onDismissMenuFinished = { selectedSong = null },
                    onDismissDetail = { showDetailSheet = false },
                    onDismissDelete = { showDeleteDialog = false },
                    onDismissRename = { showRenameDialog = false },
                    onShowDetail = { showDetailSheet = true },
                    onShowDelete = { showDeleteDialog = true },
                    onShowRename = { showRenameDialog = true },
                    onPlay = { song -> selectionViewModel.play(context, song) },
                    onDelete = { song -> selectionViewModel.delete(song) },
                    onRename = { song, newFileName ->
                        selectionViewModel.renameSong(song, newFileName)
                    }
                )
            }
        }

        YesNoDialog(
            title = stringResource(R.string.dialog_delete_album_title),
            show = showDeleteAlbumDialog,
            summary = stringResource(
                R.string.dialog_delete_album_content,
                songs.size,
                albumName.ifBlank { stringResource(R.string.album_detail_title) }
            ),
            onConfirm = {
                showDeleteAlbumDialog = false
                albumActionsViewModel.deleteAlbum(songs)
                navigator.popBackStack()
            },
            onDismissRequest = { showDeleteAlbumDialog = false }
        )

        AlbumReplayGainProgressBottomSheet(
            uiState = albumActionsUiState,
            onDismissRequest = albumActionsViewModel::closeAlbumReplayGainProgressDialog,
            onDismissFinished = albumActionsViewModel::clearAlbumReplayGainProgressDialog,
            onAbort = albumActionsViewModel::cancelAlbumReplayGain
        )

        SongBatchSelectionActions(
            navigator = navigator,
            songs = songs,
            show = isSelectionMode,
            expanded = isFabMenuExpanded,
            selectedSongUris = selectedSongUris,
            onExpandedChange = { isFabMenuExpanded = it },
            onSetSelectionUris = selectionViewModel::setSelectionUris,
            onBatchDelete = selectionViewModel::batchDelete,
            onBatchShare = selectionViewModel::batchShare
        )
    }
}

@Composable
private fun albumActionsDropdownEntry(
    isCalculatingReplayGain: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCalculateReplayGain: () -> Unit
): DropdownEntry {
    return DropdownEntry(
        items = listOf(
            DropdownItem(
                text = stringResource(R.string.menu_action_calculate_album_replay_gain),
                onClick = {
                    if (!isCalculatingReplayGain) {
                        onCalculateReplayGain()
                    }
                }
            ),
            DropdownItem(
                text = stringResource(R.string.menu_action_share_album),
                onClick = onShare
            ),
            DropdownItem(
                text = stringResource(R.string.menu_action_delete_album),
                onClick = onDelete
            )
        )
    )
}

@Composable
private fun AlbumDetailHeader(
    album: String,
    albumArtist: String?,
    songCount: Int,
    coverSong: SongEntity?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            uri = coverSong?.uri,
            lastModified = coverSong?.fileLastModified ?: 0L,
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.ifBlank { stringResource(R.string.album_detail_title) },
                style = MiuixTheme.textStyles.title3,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = albumArtist?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.unknown_album_artist),
                style = MiuixTheme.textStyles.main,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.song_count, songCount),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
