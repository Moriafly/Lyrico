package com.lonx.lyrico.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.audiotag.model.AudioPictureType
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.data.model.entity.AlbumEntity
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.ui.components.album.AlbumListItem
import com.lonx.lyrico.ui.components.bar.SongBatchSelectionActions
import com.lonx.lyrico.ui.components.bar.SongSelectionTopAppBar
import com.lonx.lyrico.ui.components.base.YesNoDialog
import com.lonx.lyrico.ui.components.CoverCandidate
import com.lonx.lyrico.ui.components.cover.CoverImage
import com.lonx.lyrico.ui.components.library.AlbumActionBottomSheet
import com.lonx.lyrico.ui.components.poster.ArtistPosterActionsSheet
import com.lonx.lyrico.ui.components.poster.ArtistPosterProgressSheet
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.ui.components.song.SongActionSheets
import com.lonx.lyrico.ui.components.song.SongListItem
import com.lonx.lyrico.ui.components.song.SongListItemActions
import com.lonx.lyrico.viewmodel.AlbumActionsViewModel
import com.lonx.lyrico.viewmodel.ArtistDetailViewModel
import com.lonx.lyrico.viewmodel.ArtistImageSource
import com.lonx.lyrico.viewmodel.SongSelectionViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AlbumDetailDestination
import com.ramcosta.composedestinations.generated.destinations.EditMetadataDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
@Destination<RootGraph>(route = "artist_detail")
fun ArtistDetailScreen(
    navigator: DestinationsNavigator,
    artistId: Long
) {
    val viewModel: ArtistDetailViewModel = koinViewModel(
        parameters = { parametersOf(artistId) }
    )
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    var selectedAlbum by remember { mutableStateOf<AlbumEntity?>(null) }
    val albumActionsViewModel: AlbumActionsViewModel = koinViewModel()
    val albumActionsUiState by albumActionsViewModel.uiState.collectAsStateWithLifecycle()

    val artistName = artist?.name.orEmpty()
    val isSelectionMode by selectionViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongUris by selectionViewModel.selectedSongUris.collectAsStateWithLifecycle()
    val swipeAnchorUri by selectionViewModel.swipeAnchorUri.collectAsStateWithLifecycle()

    var showAlbumActionSheet by remember { mutableStateOf(false) }
    var showDeleteAlbumDialog by remember { mutableStateOf(false) }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // 头像只是设置入口：用户选好去向后，再从系统选图器选本地图片。
    val posterFolder by viewModel.artistPosterFolder.collectAsStateWithLifecycle()
    val artistPosterState by viewModel.artistPosterEmbedState.collectAsStateWithLifecycle()
    var showArtistPosterSheet by remember { mutableStateOf(false) }
    var showArtistPosterProgress by remember { mutableStateOf(false) }
    var pickedArtistImage by remember { mutableStateOf<ArtistImageSource?>(null) }
    var pendingArtistPosterTarget by remember { mutableStateOf<ArtistPosterTarget?>(null) }

    fun applyPickedArtistImage(target: ArtistPosterTarget, uri: Uri?) {
        if (uri == null) return
        viewModel.prepareArtistImage(context, uri) { source ->
            pickedArtistImage = source
            showArtistPosterProgress = true
            when (target) {
                ArtistPosterTarget.EmbedIntoSongs ->
                    viewModel.embedArtistImageForAllSongs(artistName, songs, source)

                ArtistPosterTarget.SaveToFolder ->
                    viewModel.saveArtistImageToFolder(artistName, source)
            }
        }
    }

    val artistPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingArtistPosterTarget
        pendingArtistPosterTarget = null
        if (target != null) applyPickedArtistImage(target, uri)
    }

    val message = stringResource(R.string.permission_denied_cannot_save)
    // 还没有海报文件夹时先添加
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val permissionTaken = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.isSuccess
        if (!permissionTaken) {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }
        viewModel.setArtistPosterFolder(uri) {
            pendingArtistPosterTarget = ArtistPosterTarget.SaveToFolder
            artistPhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    val artistPosterPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val source = pickedArtistImage
        val name = artistName.trim()
        if (result.resultCode == Activity.RESULT_OK && source != null && name.isNotEmpty()) {
            showArtistPosterProgress = true
            viewModel.embedArtistImageForAllSongs(name, songs, source)
        } else {
            viewModel.clearArtistPosterStatus()
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 需要授权时拉起系统授权界面；只消费一次，重组不会重复弹窗
    LaunchedEffect(artistPosterState.permissionIntentSender) {
        val intentSender = artistPosterState.permissionIntentSender ?: return@LaunchedEffect
        showArtistPosterProgress = true
        artistPosterPermissionLauncher.launch(
            IntentSenderRequest.Builder(intentSender).build()
        )
        viewModel.consumeArtistPosterPermissionRequest()
    }

    val artistPosterMessage = artistPosterState.message
    LaunchedEffect(artistPosterMessage, showArtistPosterProgress) {
        if (artistPosterMessage != null && !showArtistPosterProgress) {
            Toast.makeText(context, artistPosterMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearArtistPosterMessage()
        }
    }

    fun startArtistImagePick(target: ArtistPosterTarget) {
        pendingArtistPosterTarget = target
        artistPhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
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
                    label = "ArtistDetailTopBarAnimation",
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
                            title = artistName.ifBlank { stringResource(R.string.artist_detail_title) },
                            navigationIcon = {
                                IconButton(onClick = { navigator.popBackStack() }) {
                                    Icon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = stringResource(R.string.action_back)
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
                    ArtistDetailHeader(
                        artist = artistName,
                        songCount = songs.size,
                        albums = albums,
                        coverUri = artist?.coverSongUri,
                        coverLastModified = artist?.coverSongLastModified ?: 0L,
                        coverCandidates = songs.map { song ->
                            CoverCandidate(
                                uri = song.uri.toUri(),
                                lastUpdate = song.fileLastModified
                            )
                        },
                        onAvatarClick = { showArtistPosterSheet = true }
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        TabRowWithContour(
                            tabs = listOf(
                                stringResource(R.string.artist_tab_songs),
                                stringResource(R.string.artist_tab_albums)
                            ),
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        if (page == 0) {
                            ArtistSongsPage(
                                songs = songs,
                                isSelectionMode = isSelectionMode,
                                selectedSongUris = selectedSongUris,
                                swipeSelectionLabel = swipeSelectionLabel,
                                swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                                topAppBarScrollBehavior = topAppBarScrollBehavior,
                                onSongClick = { song ->
                                    selectionViewModel.exitSelectionMode()
                                    navigator.navigate(EditMetadataDestination(songFileUri = song.uri))
                                },
                                onToggleSelection = { song ->
                                    selectionViewModel.toggleSelection(song.uri)
                                },
                                onSwipeSelection = { song ->
                                    selectionViewModel.swipeSelect(song, songs)
                                },
                                onShowSongMenu = { song ->
                                    selectedSong = song
                                    showMenuSheet = true
                                }
                            )
                        } else {
                            ArtistAlbumsPage(
                                albums = albums,
                                topAppBarScrollBehavior = topAppBarScrollBehavior,
                                onAlbumClick = { album ->
                                    selectionViewModel.exitSelectionMode()
                                    navigator.navigate(
                                        AlbumDetailDestination(albumId = album.id)
                                    )
                                },
                                onAlbumActionClick = { album ->
                                    selectedAlbum = album
                                    showAlbumActionSheet = true
                                }
                            )
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
        selectedAlbum?.let { album ->
            AlbumActionBottomSheet(
                show = showAlbumActionSheet,
                albumName = album.name,
                isCalculatingReplayGain = albumActionsUiState.isCalculatingAlbumReplayGain,
                onDismissRequest = { showAlbumActionSheet = false },
                onDismissFinished = {
                    if (!showAlbumActionSheet && !showDeleteAlbumDialog) {
                        selectedAlbum = null
                    }
                },
                onShare = {
                    showAlbumActionSheet = false
                    albumActionsViewModel.shareAlbum(context, album.id)
                },
                onDelete = {
                    showAlbumActionSheet = false
                    showDeleteAlbumDialog = true
                },
                onCalculateReplayGain = {
                    showAlbumActionSheet = false
                    albumActionsViewModel.calculateAlbumReplayGain(album.id)
                }
            )

            YesNoDialog(
                title = stringResource(R.string.dialog_delete_album_title),
                show = showDeleteAlbumDialog,
                summary = stringResource(
                    R.string.dialog_delete_album_content,
                    album.songCount,
                    album.name
                ),
                onConfirm = {
                    showDeleteAlbumDialog = false
                    albumActionsViewModel.deleteAlbum(album.id)
                    selectedAlbum = null
                },
                onDismissRequest = {
                    showDeleteAlbumDialog = false
                    selectedAlbum = null
                }
            )
        }
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

        // 点头像后选去向：内嵌到该歌手的全部歌曲，或按艺术家名存进海报文件夹
        ArtistPosterActionsSheet(
            show = showArtistPosterSheet,
            hasPosterFolder = posterFolder != null,
            onEmbedToSongs = {
                showArtistPosterSheet = false
                startArtistImagePick(ArtistPosterTarget.EmbedIntoSongs)
            },
            onSaveToFolder = {
                showArtistPosterSheet = false
                if (posterFolder == null) {
                    folderPickerLauncher.launch(null)
                } else {
                    startArtistImagePick(ArtistPosterTarget.SaveToFolder)
                }
            },
            onDismissRequest = { showArtistPosterSheet = false }
        )

        ArtistPosterProgressSheet(
            state = artistPosterState,
            show = showArtistPosterProgress && artistPosterState.hasContent,
            title = stringResource(R.string.label_set_artist_poster),
            onDismissRequest = { showArtistPosterProgress = false },
            onDismissFinished = {
                // 动画走完才丢掉结果，否则面板会先空一下再消失
                if (!artistPosterState.isRunning) {
                    showArtistPosterProgress = false
                    viewModel.clearArtistPosterStatus()
                }
            },
            onCancel = viewModel::cancelArtistPosterWrite,
            onGrantPermission = {
                artistPosterState.permissionIntentSender?.let { intentSender ->
                    artistPosterPermissionLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        )
    }
}

private enum class ArtistPosterTarget {
    EmbedIntoSongs,
    SaveToFolder
}

@Composable
private fun ArtistSongsPage(
    songs: List<SongEntity>,
    isSelectionMode: Boolean,
    selectedSongUris: Set<String>,
    swipeSelectionLabel: String,
    swipeSelectionSecondaryLabel: String?,
    topAppBarScrollBehavior: ScrollBehavior,
    onSongClick: (SongEntity) -> Unit,
    onToggleSelection: (SongEntity) -> Unit,
    onSwipeSelection: (SongEntity) -> Unit,
    onShowSongMenu: (SongEntity) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(bottom = 12.dp),
        overscrollEffect = null
    ) {
        if (songs.isEmpty()) {
            item("empty") {
                IllustratedEmptyState(
                    title = stringResource(R.string.empty_songs_title),
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(
            items = songs,
            key = { song -> song.uri.takeIf { it.isNotBlank() && it != "0" } ?: "song-${song.id}" }
        ) { song ->
            SongListItem(
                song = song,
                showTrackNumbers = true,
                isSelectionMode = isSelectionMode,
                isSelected = selectedSongUris.contains(song.uri),
                swipeSelectionLabel = swipeSelectionLabel,
                swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                onClick = { onSongClick(song) },
                onToggleSelection = { onToggleSelection(song) },
                onSwipeSelection = { onSwipeSelection(song) },
                trailingContent = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        SongListItemActions(
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedSongUris.contains(song.uri),
                            onToggleSelection = { onToggleSelection(song) },
                            onShowMenu = { onShowSongMenu(song) }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ArtistAlbumsPage(
    albums: List<AlbumEntity>,
    topAppBarScrollBehavior: ScrollBehavior,
    onAlbumClick: (AlbumEntity) -> Unit,
    onAlbumActionClick: (AlbumEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 12.dp),
        overscrollEffect = null
    ) {
        if (albums.isEmpty()) {
            item("empty") {
                IllustratedEmptyState(
                    title = stringResource(R.string.empty_albums_title),
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            AlbumListItem(
                album = album,
                onClick = { onAlbumClick(album) },
                trailingContent = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = {
                                onAlbumActionClick(album)
                            }
                        ) {
                            Icon(
                                MiuixIcons.More,
                                contentDescription = "more"
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ArtistDetailHeader(
    artist: String,
    songCount: Int,
    albums: List<AlbumEntity>,
    coverUri: String?,
    coverLastModified: Long,
    coverCandidates: List<CoverCandidate>,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图片角标沿用单曲编辑页的图片语义，同时保留整张海报的点击热区。
        Box(contentAlignment = Alignment.BottomEnd) {
            CoverImage(
                uri = coverUri,
                lastModified = coverLastModified,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = stringResource(R.string.label_set_artist_poster),
                        onClick = onAvatarClick
                    ),
                shape = CircleShape,
                contentDescription = artist,
                pictureType = AudioPictureType.Artist,
                fallbackPictureTypes = listOf(
                    AudioPictureType.LeadArtist,
                    AudioPictureType.Band
                ),
                fallbackToAny = true,
                candidates = coverCandidates,
                artistName = artist
            )
            Icon(
                imageVector = MiuixIcons.Image,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.secondaryContainer)
                    .padding(5.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.ifBlank { stringResource(R.string.artist_detail_title) },
                style = MiuixTheme.textStyles.title3,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.album_song_count,
                    albums.size,
                    songCount
                ),
                style = MiuixTheme.textStyles.main,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
