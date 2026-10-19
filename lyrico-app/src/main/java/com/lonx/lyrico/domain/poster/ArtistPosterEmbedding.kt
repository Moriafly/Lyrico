package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture

/**
 * 把一张艺术家海报写进「这位艺术家的每一首歌」。
 *
 * 归属靠图片 `description` 记录：歌曲只有这位艺术家时留空（既是唯一艺术家的既有写法，
 * 也不怕艺术家字段以后改名）；同时挂着别的艺术家时必须写名字，否则会与另一位艺术家的海报冲突。
 */
object ArtistPosterEmbedding {

    /**
     * 这首歌当前该用哪张艺术家海报；`null` 表示已经就是它，重跑时跳过。
     */
    fun plan(
        pictures: List<AudioPicture>,
        artistNames: List<String>,
        artistName: String,
        data: ByteArray,
        mimeType: String
    ): ArtistPosterBinding? {
        val key = ArtistPosterGrouping.ownerKeyOf(artistName)
        val existing = ArtistPosterGrouping.entries(pictures, artistNames)
            .firstOrNull { it.artist != null && it.ownerKey == key }
            ?.picture

        // 描述为空、而这首歌又挂着多位艺术家时无法判断它属于谁，此时当作「没有」而新增一张，
        // 免得把另一位艺术家的海报改掉。
        if (existing != null &&
            existing.data.contentEquals(data) &&
            existing.mimeType == mimeType
        ) {
            return null
        }

        return ArtistPosterBinding(
            description = if (artistNames.size <= 1) "" else artistName.trim(),
            replaced = existing
        )
    }
}

/**
 * @property description 写入目标歌曲的图片描述
 * @property replaced 被替换掉的旧海报；`null` 表示新增
 */
data class ArtistPosterBinding(
    val description: String,
    val replaced: AudioPicture?
)
