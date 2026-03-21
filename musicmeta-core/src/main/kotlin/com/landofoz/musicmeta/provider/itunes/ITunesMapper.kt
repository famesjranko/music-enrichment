package com.landofoz.musicmeta.provider.itunes

import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers
import com.landofoz.musicmeta.SearchCandidate

/** Maps iTunes DTOs to EnrichmentData subclasses. */
object ITunesMapper {

    fun toArtwork(result: ITunesAlbumResult, artworkSize: Int): EnrichmentData.Artwork? {
        val artworkUrl = result.artworkUrl ?: return null
        val highResUrl = artworkUrl.replace("100x100bb", "${artworkSize}x${artworkSize}bb")
        return EnrichmentData.Artwork(url = highResUrl, thumbnailUrl = artworkUrl)
    }

    fun toSearchCandidate(
        result: ITunesAlbumResult,
        providerId: String,
        score: Int,
    ): SearchCandidate {
        val year = result.releaseDate?.take(4)
        return SearchCandidate(
            title = result.collectionName,
            artist = result.artistName,
            year = year,
            country = result.country,
            releaseType = null,
            score = score,
            thumbnailUrl = result.artworkUrl,
            identifiers = EnrichmentIdentifiers(),
            provider = providerId,
        )
    }
}
