package com.landofoz.musicmeta.provider.deezer

import com.landofoz.musicmeta.ArtworkSize
import com.landofoz.musicmeta.DiscographyAlbum
import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers
import com.landofoz.musicmeta.SearchCandidate
import com.landofoz.musicmeta.TrackInfo

/** Maps Deezer DTOs to EnrichmentData subclasses. */
object DeezerMapper {

    fun toArtwork(result: DeezerAlbumResult): EnrichmentData.Artwork? {
        val url = result.coverXl ?: result.coverBig
            ?: result.coverMedium ?: result.coverSmall
            ?: return null
        val sizes = listOfNotNull(
            result.coverSmall?.let { ArtworkSize(url = it, width = 56, height = 56, label = "small") },
            result.coverMedium?.let { ArtworkSize(url = it, width = 250, height = 250, label = "medium") },
            result.coverBig?.let { ArtworkSize(url = it, width = 500, height = 500, label = "big") },
            result.coverXl?.let { ArtworkSize(url = it, width = 1000, height = 1000, label = "xl") },
        )
        return EnrichmentData.Artwork(
            url = url,
            thumbnailUrl = result.coverMedium,
            sizes = sizes.takeIf { it.isNotEmpty() },
        )
    }

    fun toDiscography(albums: List<DeezerArtistAlbum>): EnrichmentData.Discography =
        EnrichmentData.Discography(
            albums = albums.map { album ->
                DiscographyAlbum(
                    title = album.title,
                    year = album.releaseDate?.take(4),
                    type = album.recordType,
                    thumbnailUrl = album.coverMedium ?: album.coverSmall,
                    identifiers = EnrichmentIdentifiers().withExtra("deezerId", album.id.toString()),
                )
            },
        )

    fun toTracklist(tracks: List<DeezerTrack>): EnrichmentData.Tracklist =
        EnrichmentData.Tracklist(
            tracks = tracks.map { track ->
                TrackInfo(
                    title = track.title,
                    position = track.trackPosition,
                    durationMs = track.durationSec.toLong() * 1000,
                    identifiers = EnrichmentIdentifiers().withExtra("deezerId", track.id.toString()),
                )
            },
        )

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
