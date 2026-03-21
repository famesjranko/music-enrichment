package com.landofoz.musicmeta.provider.discogs

import com.landofoz.musicmeta.EnrichmentData

/** Maps Discogs DTOs to EnrichmentData subclasses. */
object DiscogsMapper {

    fun toArtwork(release: DiscogsRelease): EnrichmentData.Artwork? {
        val url = release.coverImage ?: return null
        return EnrichmentData.Artwork(url = url)
    }

    fun toLabelMetadata(release: DiscogsRelease): EnrichmentData.Metadata? {
        val label = release.label ?: return null
        return EnrichmentData.Metadata(label = label)
    }

    fun toReleaseTypeMetadata(release: DiscogsRelease): EnrichmentData.Metadata? {
        val releaseType = release.releaseType ?: return null
        return EnrichmentData.Metadata(releaseType = releaseType)
    }
}
