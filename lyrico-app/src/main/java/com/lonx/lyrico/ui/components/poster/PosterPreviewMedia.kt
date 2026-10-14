package com.lonx.lyrico.ui.components.poster

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.lonx.lyrico.domain.poster.ResolvedArtistPoster
import java.io.File

data class PosterPreviewMedia(
    val name: String,
    val uri: String,
    val bytes: ByteArray? = null,
    val isVideo: Boolean = false,
    val embedded: Boolean = false,
    val revision: Long = 0
) {
    // A String is stable across list updates and saveable by the pager.
    val key: String get() = if (embedded) "embedded:$uri:$name" else "file:$uri"
}

fun ResolvedArtistPoster.previewMedia(): PosterPreviewMedia = when (this) {
    is ResolvedArtistPoster.File -> PosterPreviewMedia(name, uri, if (canCrop) bytes else null, !canCrop)
    is ResolvedArtistPoster.Embedded -> PosterPreviewMedia(artistName, songUri, bytes, embedded = true)
}

internal fun PosterPreviewMedia.readBitmap(context: Context): Bitmap? = bytes?.let {
    BitmapFactory.decodeByteArray(it, 0, it.size)
} ?: context.contentResolver.openInputStream(uri.toUri())?.use { BitmapFactory.decodeStream(it) }

internal data class PosterMediaInfo(val mime: String, val width: Int, val height: Int, val size: Long?, val source: String,
    val lastModified: Long? = null)

private fun PosterPreviewMedia.modifiedTime(context: Context): Long? {
    // An embedded picture has no independent file timestamp.
    if (embedded) return null
    return try {
        context.contentResolver.query(uri.toUri(), null, null, null, null)?.use {
            if (!it.moveToFirst()) return@use null
            val documentColumn = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val mediaColumn = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
            when {
                documentColumn >= 0 && !it.isNull(documentColumn) -> it.getLong(documentColumn)
                mediaColumn >= 0 && !it.isNull(mediaColumn) -> it.getLong(mediaColumn) * 1000L
                else -> null
            }?.takeIf { time -> time > 0 }
        }
    } catch (_: Exception) { null }
}

private fun PosterPreviewMedia.sourceLocation(context: Context): String {
    val sourceUri = uri.toUri()
    return try {
        if (sourceUri.authority == "com.android.externalstorage.documents" && DocumentsContract.isDocumentUri(context, sourceUri)) {
            DocumentsContract.getDocumentId(sourceUri).substringAfter(':')
        } else {
            context.contentResolver.query(sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                val column = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && it.moveToFirst()) it.getString(column) else null
            } ?: android.net.Uri.decode(uri)
        }
    } catch (_: Exception) { android.net.Uri.decode(uri) }
}

internal fun PosterPreviewMedia.readInfo(context: Context): PosterMediaInfo {
    val source = sourceLocation(context)
    val lastModified = modifiedTime(context)
    var size: Long? = bytes?.size?.toLong()
    if (bytes == null) context.contentResolver.query(uri.toUri(), arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
        val column = it.getColumnIndex(OpenableColumns.SIZE)
        if (column >= 0 && it.moveToFirst() && !it.isNull(column)) size = it.getLong(column).takeIf { value -> value >= 0 }
    }
    if (isVideo) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri.toUri())
            return PosterMediaInfo("video/mp4",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0, size, source, lastModified)
        } finally { retriever.release() }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    else context.contentResolver.openInputStream(uri.toUri())?.use { BitmapFactory.decodeStream(it, null, bounds) }
    check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot read image dimensions" }
    return PosterMediaInfo(bounds.outMimeType ?: "image/*", bounds.outWidth, bounds.outHeight, size, source, lastModified)
}

internal fun PosterPreviewMedia.shareIntent(context: Context): Intent {
    val mime = readInfo(context).mime
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (isVideo) "mp4" else "img"
    val directory = File(context.cacheDir, "poster-shares").apply { mkdirs() }
    // Keep each granted share stable; remove old copies on a later share.
    directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }
        ?.forEach { it.delete() }
    val file = File.createTempFile("poster-", ".$extension", directory)
    try {
        file.outputStream().use { output ->
            if (bytes != null) output.write(bytes)
            else requireNotNull(context.contentResolver.openInputStream(uri.toUri())).use { it.copyTo(output) }
        }
        val shared = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, shared)
            clipData = android.content.ClipData.newRawUri(name, shared)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } catch (error: Exception) { file.delete(); throw error }
}
