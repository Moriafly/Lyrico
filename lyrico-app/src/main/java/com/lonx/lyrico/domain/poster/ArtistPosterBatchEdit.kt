package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.AudioPictureType

object ArtistPosterBatchEdit {
    fun apply(pictures: List<AudioPicture>, artistNames: List<String>, owners: Set<String>, source: AudioPicture?): List<AudioPicture> {
        if (source == null) return ArtistPosterRemoval.remove(pictures, artistNames, owners)
        val selectedKeys = owners.map(ArtistPosterGrouping::ownerKeyOf).toSet()
        var next = pictures
        for (name in artistNames.filter { ArtistPosterGrouping.ownerKeyOf(it) in selectedKeys }) {
            val binding = ArtistPosterEmbedding.plan(next, artistNames, name, source.data, source.mimeType) ?: continue
            next = ArtistPosterEdits.setFor(next, artistNames, name, source.copy(
                description = binding.description, pictureType = AudioPictureType.Artist.tagLibName
            ))
        }
        return next
    }
}
