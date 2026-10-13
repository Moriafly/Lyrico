package com.lonx.lyrico.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import com.lonx.lyrico.domain.poster.ArtistPosterFileNaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把一张海报写进艺术家海报文件夹。
 *
 * 文件夹是用户通过 SAF 授权的目录，所以写文件要经过 [DocumentsContract]，权限在授权当时就
 * 已经拿到（`takePersistableUriPermission`）。应用只允许配置一个艺术家海报文件夹。
 *
 * 文件名由 [ArtistPosterFileNaming] 按艺术家名生成。替换时先写一个完整的临时文件，再删旧文件并改名，
 * 避免把 JPEG/PNG 字节直接写进旧的 `.mp4`/`.webp` 文件，也避免写到一半破坏原海报。
 */
object ArtistPosterFileWriter {

    private const val TAG = "ArtistPosterWriter"

    /**
     * 写入 [data]；返回真正写下去的文件名，`null` 表示没有写入。
     *
     * 读写失败（磁盘、Provider 报错）与「没有可用文件夹」在这里不作区分：对用户来说都是
     * 「没存成」，界面统一提示稍后重试即可。
     */
    suspend fun write(
        context: Context,
        artistName: String,
        data: ByteArray,
        mimeType: String,
        posterFolder: String?
    ): String? = withContext(Dispatchers.IO) {
        if (data.isEmpty() || posterFolder == null) return@withContext null

        val stem = ArtistPosterFileNaming.stemFor(artistName) ?: return@withContext null
        val extension = ArtistPosterFileNaming.extensionFor(mimeType)
        // Provider 要的是具体类型；选图器偶尔给出 image/* 这类模糊值，退回 jpg 更稳妥
        val folderMimeType = mimeType.substringBefore(';').trim()
            .takeIf { it.startsWith("image/") && it != "image/*" }
            ?: "image/jpeg"

        runCatching {
            writeToFolder(
                context = context,
                folder = posterFolder,
                stem = stem,
                extension = extension,
                artistName = artistName,
                mimeType = folderMimeType,
                data = data
            )
        }.getOrElse { error ->
            Log.w(TAG, "写入海报文件夹失败: $posterFolder", error)
            null
        }
    }

    private fun writeToFolder(
        context: Context,
        folder: String,
        stem: String,
        extension: String,
        artistName: String,
        mimeType: String,
        data: ByteArray
    ): String? {
        val treeUri = folder.toUri()
        // `null` 表示文件夹被删除或授权已失效，不能与空文件夹混为一谈。
        val children = SafDocuments.children(context, treeUri) ?: return null
        val files = children.filterNot { it.isDirectory }
        val existing = ArtistPosterFileNaming
            .matchesArtist(files.map { it.displayName }, artistName)
            .firstOrNull()

        val existingUri = existing?.let { name -> files.firstOrNull { it.displayName == name }?.uri }
        val parentId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId)
        val desiredName = "$stem.$extension"
        val stagedName = if (existingUri == null) desiredName else {
            "${stem}_lyrico_${System.currentTimeMillis()}.$extension"
        }
        val documentUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentUri,
            mimeType,
            stagedName
        ) ?: return null

        val written = context.contentResolver.openOutputStream(documentUri, "wt")?.use { outputStream ->
            outputStream.write(data)
            outputStream.flush()
            true
        } ?: false

        if (!written) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, documentUri) }
            return null
        }

        if (existingUri == null) return desiredName

        // 新文件已经完整落盘，现在才动旧文件。删不掉时保留两者，至少不会丢图。
        val oldDeleted = runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, existingUri)
        }.getOrDefault(false)
        if (!oldDeleted) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, documentUri) }
            return null
        }

        val renamed = runCatching {
            DocumentsContract.renameDocument(context.contentResolver, documentUri, desiredName)
        }.getOrNull()
        return if (renamed != null) desiredName else stagedName
    }
    /** Replace an existing image only after a complete staged file has been written. */
    fun replaceFile(context: Context, tree: Uri, uri: Uri, name: String, bitmap: Bitmap, expectedBytes: ByteArray? = null) {
        val children = SafDocuments.children(context, tree) ?: error("Folder unavailable")
        check(children.any { it.uri == uri && it.displayName == name }) { "Poster changed" }
        if (expectedBytes != null) {
            val current = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            check(current != null && current.contentEquals(expectedBytes)) { "Poster changed" }
        }
        val extension = name.substringAfterLast('.').lowercase()
        val format = when (extension) {
            "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
            else -> error("Unsupported crop format")
        }
        val mime = when (extension) { "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg" }
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val token = java.util.UUID.randomUUID().toString()
        val staged = checkNotNull(DocumentsContract.createDocument(resolver, parent, mime, ".lyrico-$token.$extension"))
        var backup: Uri? = null
        var committed = false
        try {
            check(resolver.openOutputStream(staged, "wt")?.use { output ->
                bitmap.compress(format, 95, output).also { output.flush() }
            } == true)
            // Keep the original recoverable until the complete replacement has its final name.
            backup = checkNotNull(DocumentsContract.renameDocument(resolver, uri, ".lyrico-$token.bak"))
            checkNotNull(DocumentsContract.renameDocument(resolver, staged, name))
            committed = true
            runCatching { DocumentsContract.deleteDocument(resolver, backup) }
        } finally {
            if (!committed) {
                backup?.let { runCatching { DocumentsContract.renameDocument(resolver, it, name) } }
                runCatching { DocumentsContract.deleteDocument(resolver, staged) }
            }
        }
    }

}
