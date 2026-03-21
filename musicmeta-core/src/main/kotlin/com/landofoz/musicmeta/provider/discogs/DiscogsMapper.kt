package com.landofoz.musicmeta.provider.discogs

import com.landofoz.musicmeta.BandMember
import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers

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

    fun toAlbumMetadata(release: DiscogsRelease): EnrichmentData.Metadata =
        EnrichmentData.Metadata(
            label = release.label,
            releaseDate = release.year,
            releaseType = release.releaseType,
            country = release.country,
            catalogNumber = release.catno,
            genres = (release.genres.orEmpty() + release.styles.orEmpty())
                .takeIf { it.isNotEmpty() },
        )

    fun toBandMembers(artist: DiscogsArtist): EnrichmentData.BandMembers =
        EnrichmentData.BandMembers(
            members = artist.members.map { member ->
                BandMember(
                    name = member.name,
                    identifiers = EnrichmentIdentifiers()
                        .withExtra("discogsArtistId", member.id.toString()),
                )
            },
        )
}
