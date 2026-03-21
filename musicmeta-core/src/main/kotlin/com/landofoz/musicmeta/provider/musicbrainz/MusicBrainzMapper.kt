package com.landofoz.musicmeta.provider.musicbrainz

import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers

/** Maps MusicBrainz DTOs to EnrichmentData subclasses. */
object MusicBrainzMapper {

    fun toAlbumMetadata(release: MusicBrainzRelease): EnrichmentData.Metadata =
        EnrichmentData.Metadata(
            genres = release.tags.takeIf { it.isNotEmpty() },
            label = release.label,
            releaseDate = release.date,
            releaseType = release.releaseType,
            country = release.country,
            barcode = release.barcode,
            disambiguation = release.disambiguation,
        )

    fun toAlbumIdentifiers(release: MusicBrainzRelease): EnrichmentIdentifiers =
        EnrichmentIdentifiers(
            musicBrainzId = release.id,
            musicBrainzReleaseGroupId = release.releaseGroupId,
        )

    fun toArtistMetadata(artist: MusicBrainzArtist): EnrichmentData.Metadata =
        EnrichmentData.Metadata(
            genres = artist.tags.takeIf { it.isNotEmpty() },
            country = artist.country,
            disambiguation = artist.disambiguation,
            artistType = artist.type,
            beginDate = artist.beginDate,
            endDate = artist.endDate,
        )

    fun toArtistIdentifiers(artist: MusicBrainzArtist): EnrichmentIdentifiers =
        EnrichmentIdentifiers(
            musicBrainzId = artist.id,
            wikidataId = artist.wikidataId,
            wikipediaTitle = artist.wikipediaTitle,
        )

    fun toTrackMetadata(recording: MusicBrainzRecording): EnrichmentData.Metadata =
        EnrichmentData.Metadata(
            genres = recording.tags.takeIf { it.isNotEmpty() },
            isrc = recording.isrcs.firstOrNull(),
        )

    fun toTrackIdentifiers(recording: MusicBrainzRecording): EnrichmentIdentifiers =
        EnrichmentIdentifiers(musicBrainzId = recording.id)
}
