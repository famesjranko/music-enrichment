package com.landofoz.musicmeta.provider.coverartarchive

import com.landofoz.musicmeta.EnrichmentData

/** Maps Cover Art Archive responses to EnrichmentData subclasses. */
object CoverArtArchiveMapper {

    fun toArtwork(url: String, thumbnailUrl: String?): EnrichmentData.Artwork =
        EnrichmentData.Artwork(url = url, thumbnailUrl = thumbnailUrl)
}
