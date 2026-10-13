package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture

/** Only real artist artwork, never a cover or another artist's display fallback. */
sealed interface ResolvedArtistPoster {
    val artistName: String
    val bytes: ByteArray
    val canCrop: Boolean

    data class Embedded(
        override val artistName: String,
        val songUri: String,
        val picture: AudioPicture
    ) : ResolvedArtistPoster {
        override val bytes: ByteArray get() = picture.data
        override val canCrop: Boolean get() = true
    }

    data class File(
        override val artistName: String,
        val folderUri: String,
        val uri: String,
        val name: String,
        override val bytes: ByteArray
    ) : ResolvedArtistPoster {
        override val canCrop: Boolean get() = !name.endsWith(".mp4", true)
    }
}
