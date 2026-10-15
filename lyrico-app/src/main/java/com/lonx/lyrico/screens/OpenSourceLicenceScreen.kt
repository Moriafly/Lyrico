package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.FilledIcons
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode
import my.nanihadesuka.compose.ScrollbarSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
@Destination<RootGraph>(route = "opensource_licence")
fun OpenSourceLicenceScreen(
    navigator: DestinationsNavigator
) {
    val libsState = produceLibraries(R.raw.aboutlibraries)
    val libs = libsState.value
    val libraries = libs?.libraries
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()
    Scaffold(
        topBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                SmallTopAppBar(
                    color = Color.Transparent,
                    defaultWindowInsetsPadding = false,
                    title = stringResource(id = R.string.title_opensource_licence),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = FilledIcons.ArrowLeft,
                                contentDescription = null
                            )
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        }
    ) { paddingValues ->
        LazyColumnScrollbar(
            state = listState,
            settings = ScrollbarSettings.Default.copy(
                selectionMode = ScrollbarSelectionMode.Full,
                thumbUnselectedColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
                thumbSelectedColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .blurSource(topBarBackdrop)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fillMaxHeight(),
                contentPadding = scaffoldContentPadding(
                    paddingValues = paddingValues,
                    bottomExtra = 12.dp
                ),
                overscrollEffect = null,
            ) {
                libraries?.let { libs ->
                    items(libs, key = { it.uniqueId }) { library ->
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 12.dp),
                        ) {
                            ArrowPreference(
                                title = library.name,
                                summary = buildString {
                                    append(library.artifactVersion)

                                    val licenseNames = library.licenses
                                        .map { it.name }
                                        .filter { it.isNotBlank() }

                                    if (licenseNames.isNotEmpty()) {
                                        append("\n")
                                        append(licenseNames.joinToString(", "))
                                    }
                                },
                                onClick = {
                                    library.website?.let {
                                        uriHandler.openUri(it)
                                    }
                                }
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
