package com.lonx.lyrico.ui.components.poster

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.lonx.lyrico.R
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More

/** Shared full-screen poster preview, including zoom and optional video/file actions. */
@Composable
fun ArtistPosterPreview(
    pageCount: Int,
    initialPage: Int = 0,
    title: (Int) -> String,
    pageKey: (Int) -> Any,
    videoUri: (Int) -> String? = { null },
    onMore: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    imageContent: @Composable (Int, Modifier) -> Unit
) {
    val pager = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(MiuixIcons.Back, stringResource(R.string.artist_poster_back), tint = Color.White) }
                Text(title(pager.currentPage), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                if (pageCount > 1) Text("${pager.currentPage + 1} / $pageCount", color = Color.White, modifier = Modifier.padding(start = 12.dp))
                if (onMore != null) IconButton(onClick = { onMore(pager.currentPage) }) {
                    Icon(MiuixIcons.More, stringResource(R.string.cd_more_actions), tint = Color.White)
                }
            }
            HorizontalPager(pager, modifier = Modifier.fillMaxWidth().weight(1f), key = pageKey) { page ->
                val video = videoUri(page)
                if (video != null && page == pager.currentPage) {
                    var failed by remember(pageKey(page)) { mutableStateOf(false) }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setVideoURI(video.toUri())
                                    setMediaController(MediaController(context).also { it.setAnchorView(this) })
                                    setOnPreparedListener { start() }
                                    setOnErrorListener { _, _, _ -> failed = true; true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            onRelease = { it.stopPlayback() }
                        )
                        if (failed) Text(stringResource(R.string.artist_poster_preview_error), color = Color.White)
                    }
                } else {
                    var zoom by remember(pageKey(page)) { mutableFloatStateOf(1f) }
                    var offset by remember(pageKey(page)) { mutableStateOf(Offset.Zero) }
                    val transform = rememberTransformableState { zoomChange, pan, _ ->
                        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
                        offset = if (zoom > 1f) offset + pan else Offset.Zero
                    }
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) {
                        imageContent(page, Modifier.fillMaxSize()
                            .transformable(transform, canPan = { zoom > 1f })
                            .graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = offset.x; translationY = offset.y })
                    }
                }
            }
        }
    }
}
