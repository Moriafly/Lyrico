package com.lonx.lyrico.ui.components.crop

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun ImageCropperSheet(
    show: Boolean,
    bitmap: Bitmap?,
    title: String,
    onCrop: (Bitmap) -> Unit,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit
) {
    val state = bitmap?.let { rememberImageCropperState(it) }
    WindowBottomSheet(
        show = show, title = title, enableNestedScroll = false,
        onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
        endAction = {
            IconButton(enabled = state != null, onClick = {
                state?.crop()?.let(onCrop)
                onDismissRequest()
            }) { Icon(MiuixIcons.Ok, stringResource(R.string.confirm)) }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            if (state != null) ImageCropper(state, Modifier.fillMaxWidth())
            else Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
