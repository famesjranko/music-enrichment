package com.landofoz.musicmeta.provider.deezer

import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers
import com.landofoz.musicmeta.SearchCandidate

/** Maps Deezer DTOs to EnrichmentData subclasses. */
object DeezerMapper {

    fun toArtwork(result: DeezerAlbumResult): EnrichmentData.Artwork? {
        val url = result.coverXl ?: result.coverBig
            ?: result.coverMedium ?: result.coverSmall
            ?: return null
        return EnrichmentData.Artwork(url = url, thumbnailUrl = result.coverMedium)
    }

    fun toSearchCandidate(
        result: DeezerAlbumResult,
        providerId: String,
        score: Int,
    ): SearchCandidate = SearchCandidate(
        title = result.title,
        artist = result.artistName,
        year = null,
        country = null,
        releaseType = null,
        score = score,
        thumbnailUrl = result.coverMedium ?: result.coverSmall,
        identifiers = EnrichmentIdentifiers(),
        provider = providerId,
    )
}
