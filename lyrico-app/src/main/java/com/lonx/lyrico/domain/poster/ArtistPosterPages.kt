package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture

/**
 * 封面区艺术家区域的一页：空 [artistName] 表示艺术家字段未填，[entry] 为 null 时只能靠外置海报。
 */
data class ArtistPosterPage(
    val artistName: String,
    val entry: ArtistPictureEntry?,
    val showExternalPoster: Boolean
)

/**
 * 决定艺术家区域显示哪几页。内嵌海报与外置文件夹是同一「一位艺术家一张海报」的两个来源，
 * 必须按同一套归属规则分配；外置文件夹按文件名认人，整段 `A/B/C` 匹配不到 `A.jpg`。
 */
object ArtistPosterPages {

    /**
     * 一位艺术家一个海报位：
     * 1. 按艺术家字段顺序列出各自的内嵌海报；
     * 2. 没有内嵌海报时交给外置文件夹兜底，但原始标签里本来有、被用户删掉的留空；
     * 3. 描述对不上任何艺术家的海报各占一页；一个位都没有时留一个空位供添加。
     */
    fun build(
        pictures: List<AudioPicture>,
        original: List<AudioPicture>,
        artistNames: List<String>
    ): List<ArtistPosterPage> {
        val entries = ArtistPosterGrouping.entries(pictures, artistNames)
        val originalKeys = ArtistPosterGrouping.entries(original, artistNames)
            .map { it.ownerKey }
            .toSet()

        val pages = mutableListOf<ArtistPosterPage>()
        artistNames.forEach { artistName ->
            val key = ArtistPosterGrouping.ownerKeyOf(artistName)
            val owned = entries.filter { it.artist != null && it.ownerKey == key }
            if (owned.isNotEmpty()) {
                owned.forEach { pages += ArtistPosterPage(artistName, it, showExternalPoster = false) }
            } else {
                pages += ArtistPosterPage(
                    artistName = artistName,
                    entry = null,
                    showExternalPoster = key !in originalKeys
                )
            }
        }

        entries.filter { it.artist == null }
            .forEach { pages += ArtistPosterPage(it.ownerName, it, showExternalPoster = false) }

        if (pages.isEmpty()) {
            pages += ArtistPosterPage(artistName = "", entry = null, showExternalPoster = true)
        }
        return pages
    }
}
