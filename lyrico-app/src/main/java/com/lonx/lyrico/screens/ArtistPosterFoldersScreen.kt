package com.lonx.lyrico.screens

import top.yukonga.miuix.kmp.icon.extended.AddFolder
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.ArtistPosterFoldersViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ArtistDetailDestination
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import com.lonx.lyrico.viewmodel.ArtistPosterFile
import com.lonx.lyrico.ui.components.base.YesNoDialog

@Composable
@Destination<RootGraph>(route = "artist_poster_folders")
fun ArtistPosterFoldersScreen(navigator: DestinationsNavigator) {
    val viewModel: ArtistPosterFoldersViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            viewModel.addFolder(it)
        }
    }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showFolderDetails by rememberSaveable { mutableStateOf(false) }
    var actionFile by remember { mutableStateOf<ArtistPosterFile?>(null) }
    var showFileMenu by remember { mutableStateOf(false) }
    var previewUri by remember(state.folder?.uri) { mutableStateOf<String?>(null) }
    var showDeleteFile by remember { mutableStateOf(false) }
    var showRenameFile by remember { mutableStateOf(false) }
    var renameStem by remember { mutableStateOf("") }
    var cropFile by remember { mutableStateOf<ArtistPosterFile?>(null) }
    var cropBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showCrop by remember { mutableStateOf(false) }
    LaunchedEffect(state.operationMessage) {
        state.operationMessage?.let { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
        viewModel.clearOperationMessage()
    }
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                SmallTopAppBar(
                    title = stringResource(R.string.artist_poster_folders),
                    color = Color.Transparent,
                    defaultWindowInsetsPadding = false,
                    navigationIcon = {
                        IconButton(onClick = navigator::navigateUp) {
                            Icon(MiuixIcons.Back, stringResource(R.string.artist_poster_back))
                        }
                    },
                    actions = {
                        OverlayIconDropdownMenu(
                            entry = DropdownEntry(
                                items = buildList {
                                    add(DropdownItem(
                                        text = stringResource(R.string.artist_poster_naming),
                                        onClick = { showHelp = true }
                                    ))
                                    if (state.folder != null) {
                                        add(DropdownItem(
                                            text = stringResource(R.string.artist_poster_folder_details),
                                            onClick = { showFolderDetails = true }
                                        ))
                                        add(DropdownItem(
                                            text = stringResource(R.string.action_refresh_folder),
                                            onClick = viewModel::refresh
                                        ))
                                        add(DropdownItem(
                                            text = stringResource(R.string.artist_poster_folder_change),
                                            onClick = { picker.launch(null) }
                                        ))
                                        add(DropdownItem(
                                            text = stringResource(R.string.folder_action_remove),
                                            onClick = { showRemoveDialog = true }
                                        ))
                                    }
                                }
                            ),
                            enabled = !state.isLoading && !state.isEditing
                        ) {
                            Icon(MiuixIcons.More, stringResource(R.string.cd_more_actions))
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().blurSource(topBarBackdrop)) {
            val folder = state.folder
            if (folder != null) {
                ArtistPosterFolderContents(
                    folder = folder,
                    revision = state.revision,
                    isLoading = state.isLoading,
                    enabled = !state.isEditing,
                    hasError = state.error,
                    onChangeFolder = { picker.launch(null) },
                    onRefresh = viewModel::refresh,
                    onFileMenu = { previewUri = null; actionFile = it; showFileMenu = true },
                    previewUri = previewUri,
                    onDismissPreview = { previewUri = null },
                    padding = scaffoldContentPadding(padding, horizontalExtra = 12.dp, bottomExtra = 12.dp),
                    scrollBehavior = scrollBehavior
                )
            } else {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize().padding(scaffoldContentPadding(padding)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Folders,
                        title = stringResource(R.string.artist_poster_folder_not_selected),
                        summary = stringResource(if (state.error) R.string.artist_poster_folder_error else R.string.artist_poster_folders_empty),
                        icon = MiuixIcons.AddFolder,
                        actionText = stringResource(R.string.artist_poster_folder_add),
                        onAction = { picker.launch(null) },
                        modifier = Modifier.fillMaxSize().padding(scaffoldContentPadding(padding))
                    )
                }
            }
        }
        val file = actionFile
        com.lonx.lyrico.ui.components.base.ItemMenuBottomSheet(
            show = showFileMenu, label = file?.name.orEmpty(), enableNestedScroll = false,
            onDismissRequest = { showFileMenu = false }
        ) {
            ArrowPreference(
                title = stringResource(R.string.poster_view_artwork),
                enabled = file != null && !state.isEditing,
                onClick = { showFileMenu = false; previewUri = file?.uri }
            )
            ArrowPreference(
                title = stringResource(R.string.poster_artist_details),
                summary = if (file?.artistId == null) stringResource(R.string.poster_artist_unmatched) else null,
                enabled = file?.artistId != null && !state.isLoading && !state.isEditing,
                onClick = {
                    file?.artistId?.let { artistId ->
                        showFileMenu = false
                        navigator.navigate(ArtistDetailDestination(artistId = artistId))
                    }
                }
            )
            ArrowPreference(title = stringResource(R.string.action_rename), enabled = !state.isEditing, onClick = {
                renameStem = file?.name?.substringBeforeLast('.').orEmpty()
                showFileMenu = false; showRenameFile = true
            })
            if (file?.name?.substringAfterLast('.')?.lowercase() in listOf("jpg", "jpeg", "png", "webp")) {
                ArrowPreference(title = stringResource(R.string.label_crop_artist_image), enabled = !state.isEditing, onClick = {
                    showFileMenu = false
                    cropFile = file
                    file?.let { viewModel.readBitmap(it) { bitmap -> cropBitmap = bitmap; showCrop = true } }
                })
            }
            ArrowPreference(title = stringResource(R.string.action_delete), enabled = !state.isEditing,
                titleColor = BasicComponentColors(MiuixTheme.colorScheme.error, MiuixTheme.colorScheme.disabledOnSecondaryVariant),
                onClick = { showFileMenu = false; showDeleteFile = true })
        }
        YesNoDialog(
            show = showDeleteFile && file != null, title = stringResource(R.string.dialog_delete_file_title),
            summary = stringResource(R.string.dialog_delete_file_content, file?.name.orEmpty()),
            onDismissRequest = { showDeleteFile = false },
            onConfirm = { file?.let(viewModel::deleteFile) }
        )
        val proposedName = file?.let { target ->
            com.lonx.lyrico.domain.poster.PosterFileEditNames.rename(target.name, renameStem,
                state.folder?.posters.orEmpty().filter { it.uri != target.uri }.map { it.name })
        }
        YesNoDialog(
            show = showRenameFile && file != null, title = stringResource(R.string.dialog_rename_title),
            confirmEnabled = proposedName != null && proposedName != file?.name && !state.isEditing,
            onDismissRequest = { showRenameFile = false },
            onConfirm = { file?.let { viewModel.renameFile(it, renameStem) } },
            content = {
                TextField(value = renameStem, onValueChange = { renameStem = it }, maxLines = 1,
                    modifier = Modifier.fillMaxWidth(), trailingIcon = {
                        Text(".${file?.name?.substringAfterLast('.').orEmpty()}", modifier = Modifier.padding(horizontal = 12.dp))
                    })
                Text(stringResource(R.string.poster_rename_hint), style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 8.dp))
                if (proposedName == null) Text(stringResource(R.string.poster_rename_invalid),
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp))
            }
        )
        com.lonx.lyrico.ui.components.crop.ImageCropperSheet(
            show = showCrop, bitmap = cropBitmap, title = stringResource(R.string.label_crop_artist_image),
            onCrop = { bitmap -> cropFile?.let { viewModel.cropFile(it, bitmap) } },
            onDismissRequest = { showCrop = false },
            onDismissFinished = { if (!showCrop) { cropBitmap = null; cropFile = null } }
        )
        WindowDialog(
            title = stringResource(R.string.artist_poster_naming),
            show = showHelp,
            onDismissRequest = { showHelp = false }
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.artist_poster_folders_summary),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                MarkdownText(
                    modifier = Modifier.fillMaxWidth(),
                    markdown = stringResource(R.string.artist_poster_folders_help),
                    linkColor = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2.copy(
                        color = MiuixTheme.colorScheme.onSurface
                    )
                )
            }
        }
        val folder = state.folder
        if (folder != null) {
            WindowDialog(
                title = stringResource(R.string.artist_poster_folder_details),
                show = showFolderDetails,
                onDismissRequest = { showFolderDetails = false }
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(folder.name, style = MiuixTheme.textStyles.title4)
                    Text(
                        folder.path,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        stringResource(R.string.artist_poster_folder_status, folder.posters.orEmpty().size, folder.matchedPosterCount),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
        if (showRemoveDialog && folder != null) {
            WindowDialog(
                title = stringResource(R.string.dialog_remove_folder_title),
                show = true,
                onDismissRequest = { showRemoveDialog = false }
            ) {
                Column {
                    Text(folder.path, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.artist_poster_folder_remove_tip),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            text = stringResource(R.string.cancel),
                            onClick = { showRemoveDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(20.dp))
                        TextButton(
                            text = stringResource(R.string.confirm),
                            onClick = {
                                viewModel.removeFolder()
                                showRemoveDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
    }
}
