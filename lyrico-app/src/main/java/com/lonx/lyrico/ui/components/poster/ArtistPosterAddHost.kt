package com.lonx.lyrico.ui.components.poster

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.viewmodel.ArtistDetailViewModel
import com.lonx.lyrico.viewmodel.ArtistImageSource

/** Shared local image picker, destination, permission retry and write progress. */
@Composable
fun ArtistPosterAddHost(
    viewModel: ArtistDetailViewModel,
    show: Boolean,
    onDismissRequest: () -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artistName = artist?.name.orEmpty()
    val posterFolder by viewModel.artistPosterFolder.collectAsStateWithLifecycle()
    val artistPosterState by viewModel.artistPosterEmbedState.collectAsStateWithLifecycle()
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

    com.lonx.lyrico.ui.components.base.ItemMenuBottomSheet(
        show = show,
        label = stringResource(R.string.artist_poster_destination_title),
        enableNestedScroll = false,
        onDismissRequest = onDismissRequest
    ) {
        ArtistPosterDestinationPreferences(
            hasPosterFolder = posterFolder != null,
            enabled = enabled && !artistPosterState.isRunning && artist != null && songs.isNotEmpty(),
            onEmbedToSongs = {
                onDismissRequest()
                startArtistImagePick(ArtistPosterTarget.EmbedIntoSongs)
            },
            onSaveToFolder = {
                onDismissRequest()
                if (posterFolder == null) folderPickerLauncher.launch(null)
                else startArtistImagePick(ArtistPosterTarget.SaveToFolder)
            }
        )
    }
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

private enum class ArtistPosterTarget { EmbedIntoSongs, SaveToFolder }
