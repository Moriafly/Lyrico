package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture

object ArtistPosterRemoval {
    fun remove(pictures: List<AudioPicture>, artistNames: List<String>, owners: Set<String>): List<AudioPicture> {
        val keys = owners.map(ArtistPosterGrouping::ownerKeyOf).toSet()
        return pictures.filterNot { ArtistPosterGrouping.ownerKeyOf(it, artistNames) in keys }
    }
}
