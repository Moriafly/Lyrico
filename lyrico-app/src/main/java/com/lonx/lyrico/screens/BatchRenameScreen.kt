package com.lonx.lyrico.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.data.model.RenamePreview
import com.lonx.lyrico.ui.components.scaffoldBottomPadding
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.ramcosta.composedestinations.generated.destinations.CharacterMappingDestination
import com.lonx.lyrico.utils.TagField
import com.lonx.lyrico.viewmodel.BatchRenameViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent

private enum class BatchRenameTab(val labelRes: Int) {
    Config(R.string.batch_rename_tab_config),
    Preview(R.string.batch_rename_tab_preview)
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
@Destination<RootGraph>(route = "batch_rename")
fun BatchRenameScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: BatchRenameViewModel = koinViewModel()
    val renameFormat by viewModel.renameFormat.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showDropdowns = remember { mutableStateOf(false) }
    val tabs = remember { BatchRenameTab.entries }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    var showPlaceholderInfo by remember { mutableStateOf(false) }

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(id = R.string.batch_rename_title),
                navigationIcon = {
                    IconButton(
                        onClick = { if (!uiState.isRenamingInProgress) navigator.popBackStack() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!uiState.isRenamingInProgress) {
                                navigator.navigate(CharacterMappingDestination)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.ListView,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldTopHorizontalPadding(paddingValues))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            ) {
                val tabLabels = tabs.map { tab ->
                    when (tab) {
                        BatchRenameTab.Config -> stringResource(tab.labelRes)
                        BatchRenameTab.Preview -> stringResource(tab.labelRes, uiState.previews.size)
                    }
                }
                TabRowWithContour(
                    tabs = tabLabels,
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> tabs[index].name }
            ) { page ->
                when (tabs[page]) {
                    BatchRenameTab.Config -> BatchRenameConfigTab(
                        bottomPadding = scaffoldBottomPadding(paddingValues),
                        topAppBarNestedScrollConnection = topAppBarScrollBehavior.nestedScrollConnection,
                        renameFormat = renameFormat,
                        uiState = uiState,
                        showDropdowns = showDropdowns.value,
                        onDropdownVisibleChange = { showDropdowns.value = it },
                        showPlaceholderInfo = showPlaceholderInfo,
                        onShowPlaceholderInfoChange = { showPlaceholderInfo = it },
                        onFormatChange = viewModel::saveFormat,
                        onExecuteRename = viewModel::executeRename,
                        errorMessage = uiState.errorMessage?.asString(context)
                    )

                    BatchRenameTab.Preview -> BatchRenamePreviewTab(
                        bottomPadding = scaffoldBottomPadding(paddingValues),
                        topAppBarNestedScrollConnection = topAppBarScrollBehavior.nestedScrollConnection,
                        uiState = uiState
                    )
                }
            }
        }

        // 重命名进度对话框
        WindowBottomSheet(
            show = uiState.renameProgress != null,
            onDismissRequest = {
                if (!uiState.isRenamingInProgress) viewModel.closeRenameDialog()
            },
            allowDismiss = !uiState.isRenamingInProgress,
            title = stringResource(R.string.batch_rename_title),
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.renameProgress?.let { (current, total) ->
                        val progress =
                            if (total > 0) current.toFloat() / total.toFloat() else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.isRenamingInProgress) {
                                    uiState.currentFile
                                } else {
                                    stringResource(
                                        R.string.batch_matching_total_time,
                                        uiState.renameTimeMillis / 1000.0
                                    )
                                },
                                style = MiuixTheme.textStyles.subtitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "$current / $total",
                                style = MiuixTheme.textStyles.main,
                                textAlign = TextAlign.End
                            )
                        }

                        LinearProgressIndicator(progress = progress)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.batch_matching_success,
                            uiState.successCount
                        ),
                        style = MiuixTheme.textStyles.main
                    )
                    Text(
                        text = stringResource(
                            R.string.batch_replay_gain_skipped,
                            uiState.skippedCount
                        ),
                        style = MiuixTheme.textStyles.main
                    )
                    Text(
                        text = stringResource(
                            R.string.batch_matching_failure,
                            uiState.failureCount
                        ),
                        style = MiuixTheme.textStyles.main
                    )
                }

                TextButton(
                    text = if (uiState.isRenamingInProgress) stringResource(R.string.action_abort) else stringResource(
                        R.string.confirm
                    ),
                    onClick = {
                        if (uiState.isRenamingInProgress) {
                            viewModel.abortRename()
                        } else {
                            viewModel.closeRenameDialog()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )

            }
        }
    }
}

@Composable
private fun BatchRenameConfigTab(
    bottomPadding: androidx.compose.ui.unit.Dp,
    topAppBarNestedScrollConnection: NestedScrollConnection,
    renameFormat: String,
    uiState: com.lonx.lyrico.viewmodel.BatchRenameUiState,
    showDropdowns: Boolean,
    onDropdownVisibleChange: (Boolean) -> Unit,
    showPlaceholderInfo: Boolean,
    onShowPlaceholderInfoChange: (Boolean) -> Unit,
    onFormatChange: (String) -> Unit,
    onExecuteRename: () -> Unit,
    errorMessage: String?
) {
    LazyColumn(
        modifier = Modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(topAppBarNestedScrollConnection)
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomPadding + 12.dp),
        overscrollEffect = null,
    ) {
        item {
            SmallTitle(text = stringResource(id = R.string.rename_format))
            Card(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                TextField(
                    modifier = Modifier.padding(12.dp),
                    value = renameFormat,
                    onValueChange = onFormatChange,
                    trailingIcon = {
                        Box {
                            IconButton(
                                modifier = Modifier.padding(end = 12.dp),
                                onClick = { onDropdownVisibleChange(true) }) {
                                Icon(
                                    imageVector = MiuixIcons.Notes,
                                    contentDescription = null
                                )
                            }
                            OverlayListPopup(
                                show = showDropdowns,
                                popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                                onDismissRequest = { onDropdownVisibleChange(false) },
                                alignment = PopupPositionProvider.Align.TopEnd
                            ) {
                                ListPopupColumn {
                                    uiState.presetFormats.forEach { format ->
                                        DropdownImpl(
                                            text = format,
                                            isSelected = renameFormat == format,
                                            optionSize = uiState.presetFormats.size,
                                            index = uiState.presetFormats.indexOf(format),
                                            onSelectedIndexChange = {
                                                onFormatChange(format)
                                                onDropdownVisibleChange(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = stringResource(id = R.string.format_hint),
                    fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
                SwitchPreference(
                    title = stringResource(id = R.string.format_preset_show_placeholders),
                    checked = showPlaceholderInfo,
                    onCheckedChange = onShowPlaceholderInfoChange
                )
                AnimatedVisibility(visible = showPlaceholderInfo) {
                    PlaceholderInfoContent()
                }

                TextButton(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    onClick = onExecuteRename,
                    text = stringResource(id = R.string.action_rename),
                    enabled = uiState.previews.isNotEmpty() && !uiState.isRenamingInProgress,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }

        errorMessage?.let { text ->
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = text,
                        fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchRenamePreviewTab(
    bottomPadding: androidx.compose.ui.unit.Dp,
    topAppBarNestedScrollConnection: NestedScrollConnection,
    uiState: com.lonx.lyrico.viewmodel.BatchRenameUiState
) {
    var expandedPreviewKey by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(topAppBarNestedScrollConnection)
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomPadding + 12.dp),
        overscrollEffect = null,
    ) {
        item {
            SmallTitle(
                text = stringResource(
                    if (uiState.isGeneratingPreview)
                        R.string.preview_title_generating
                    else
                        R.string.preview_title,
                    uiState.previews.size
                )
            )
        }

        if (uiState.previews.isEmpty()) {
            item {
                IllustratedEmptyState(
                    kind = EmptyStateKind.Tasks,
                    title = stringResource(R.string.preview_empty_tip),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            itemsIndexed(
                items = uiState.previews,
                key = { _, preview -> preview.key }
            ) { _, preview ->
                val previewKey = preview.key
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .animateContentSize()
                ) {
                    BasicComponent(
                        onClick = {
                            expandedPreviewKey = if (expandedPreviewKey == previewKey) null else previewKey
                        }
                    ) {
                        Text(
                            text = stringResource(
                                R.string.label_old_name,
                                preview.originalPath.substringAfterLast('/')
                            ),
                            style = MiuixTheme.textStyles.body1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = stringResource(
                                R.string.label_new_name,
                                preview.newPath.substringAfterLast('/')
                            ),
                            style = MiuixTheme.textStyles.body1.copy(if (preview.conflict) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (preview.conflict) {
                            Text(
                                text = stringResource(R.string.rename_conflict_warning),
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        AnimatedVisibility(visible = expandedPreviewKey == previewKey) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_old_path, preview.originalPath),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                                Text(
                                    text = stringResource(R.string.label_new_path, preview.newPath),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = if (preview.conflict) {
                                        MiuixTheme.colorScheme.error
                                    } else {
                                        MiuixTheme.colorScheme.onSurfaceVariantActions
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private val RenamePreview.key: String
    get() = "$originalPath\n$newPath"

@Composable
private fun PlaceholderInfoContent() {
    val placeholders = TagField.entries.map {
        "@${it.index}" to it.description
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        placeholders.forEach { (placeholder, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(description),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
        }
    }
}
