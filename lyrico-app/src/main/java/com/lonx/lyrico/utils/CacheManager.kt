package com.lonx.lyrico.utils

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.lonx.lyrico.data.model.cache.CacheCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheManager {

    private const val TAG = "CacheManager"

    /** 按目录分类的缓存大小（字节）。 */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun getCategorizedCacheSize(
        context: Context
    ): Map<CacheCategory, Long> = withContext(Dispatchers.IO) {

        val cacheMap = mutableMapOf<CacheCategory, Long>()

        val coilCacheFile = context.imageLoader.diskCache?.directory?.toFile()
        val coilSize = coilCacheFile?.getFolderSize() ?: 0L
        cacheMap[CacheCategory.IMAGE] = coilSize

        val internalCacheDir = context.cacheDir
        var networkSize = 0L
        var otherInternalSize = 0L

        internalCacheDir.listFiles()?.forEach { file ->
            when {
                coilCacheFile != null &&
                        file.absolutePath == coilCacheFile.absolutePath -> {
                    // Coil 缓存已单独统计
                }

                file.name.contains("http", true) ||
                        file.name.contains("network", true) -> {
                    networkSize += file.getFolderSize()
                }

                else -> {
                    otherInternalSize += file.getFolderSize()
                }
            }
        }

        cacheMap[CacheCategory.NETWORK] = networkSize
        cacheMap[CacheCategory.OTHER] = otherInternalSize

        val externalSize = context.externalCacheDir?.getFolderSize() ?: 0L
        cacheMap[CacheCategory.EXTERNAL] = externalSize

        cacheMap
    }

    /** 总大小。 */
    fun getTotalSize(map: Map<CacheCategory, Long>): Long =
        map.values.sum()

    /** 清理指定类别的缓存。 */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearCacheByCategory(
        context: Context,
        category: CacheCategory
    ) = withContext(Dispatchers.IO) {

        when (category) {

            CacheCategory.IMAGE -> {
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
            }

            CacheCategory.NETWORK -> {
                context.cacheDir.listFiles()
                    ?.filter {
                        it.name.contains("http", true) ||
                                it.name.contains("network", true)
                    }
                    ?.forEach { it.deleteRecursively() }
            }

            CacheCategory.EXTERNAL -> {
                context.externalCacheDir?.deleteRecursively()
            }

            CacheCategory.OTHER -> {
                val coilPath =
                    context.imageLoader.diskCache?.directory?.toString()

                context.cacheDir.listFiles()?.forEach { file ->
                    val isImage =
                        coilPath != null && file.absolutePath == coilPath

                    val isNetwork =
                        file.name.contains("http", true) ||
                                file.name.contains("network", true)

                    if (!isImage && !isNetwork) {
                        file.deleteRecursively()
                    }
                }
            }
        }
    }

    /** 清理全部缓存。 */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearAllCache(context: Context) =
        withContext(Dispatchers.IO) {
            context.imageLoader.diskCache?.clear()
            context.imageLoader.memoryCache?.clear()
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        }
}

/** 递归计算文件夹大小。 */
fun File.getFolderSize(): Long {
    if (!this.exists()) return 0L
    if (this.isFile) return this.length()

    return this.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
}

