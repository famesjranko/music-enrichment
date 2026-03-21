package com.landofoz.musicmeta.provider.discogs

import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers
import com.landofoz.musicmeta.EnrichmentProvider
import com.landofoz.musicmeta.EnrichmentRequest
import com.landofoz.musicmeta.EnrichmentResult
import com.landofoz.musicmeta.EnrichmentType
import com.landofoz.musicmeta.ErrorKind
import com.landofoz.musicmeta.ProviderCapability
import com.landofoz.musicmeta.engine.ArtistMatcher
import com.landofoz.musicmeta.engine.ConfidenceCalculator
import com.landofoz.musicmeta.http.HttpClient
import com.landofoz.musicmeta.http.RateLimiter

/**
 * Discogs enrichment provider. Searches for album releases to supply
 * cover art and label metadata, and artist endpoints for band members.
 * Requires a Discogs personal access token.
 */
class DiscogsProvider(
    private val tokenProvider: () -> String,
    httpClient: HttpClient,
    rateLimiter: RateLimiter,
) : EnrichmentProvider {

    constructor(personalToken: String, httpClient: HttpClient, rateLimiter: RateLimiter) :
        this({ personalToken }, httpClient, rateLimiter)

    private val api = DiscogsApi(tokenProvider, httpClient, rateLimiter)

    override val id = "discogs"
    override val displayName = "Discogs"
    override val requiresApiKey = true
    override val isAvailable: Boolean get() = tokenProvider().isNotBlank()

    override val capabilities = listOf(
        ProviderCapability(EnrichmentType.ALBUM_ART, priority = 20),
        ProviderCapability(EnrichmentType.LABEL, priority = 50),
        ProviderCapability(EnrichmentType.RELEASE_TYPE, priority = 50),
        ProviderCapability(EnrichmentType.BAND_MEMBERS, priority = 50),
        ProviderCapability(EnrichmentType.ALBUM_METADATA, priority = 40),
    )

    override suspend fun enrich(
        request: EnrichmentRequest,
        type: EnrichmentType,
    ): EnrichmentResult {
        if (!isAvailable) return EnrichmentResult.NotFound(type, id)

        if (type == EnrichmentType.BAND_MEMBERS) {
            val artistRequest = request as? EnrichmentRequest.ForArtist
                ?: return EnrichmentResult.NotFound(type, id)
            return enrichBandMembers(artistRequest, type)
        }

        val albumRequest = request as? EnrichmentRequest.ForAlbum
            ?: return EnrichmentResult.NotFound(type, id)

        return try {
            // Discogs titles are "Artist - Title"; verify artist matches
            val releases = api.searchReleases(albumRequest.title, albumRequest.artist)
            val release = releases.firstOrNull {
                val discogsArtist = it.title.substringBefore(" - ").trim()
                ArtistMatcher.isMatch(albumRequest.artist, discogsArtist)
            } ?: releases.firstOrNull()
                ?: return EnrichmentResult.NotFound(type, id)
            enrichFromRelease(release, type)
        } catch (e: Exception) {
            mapError(type, e)
        }
    }

    private fun mapError(type: EnrichmentType, e: Exception): EnrichmentResult.Error {
        val kind = when (e) {
            is java.io.IOException -> ErrorKind.NETWORK
            is org.json.JSONException -> ErrorKind.PARSE
            else -> ErrorKind.UNKNOWN
        }
        return EnrichmentResult.Error(type, id, e.message ?: "Unknown error", e, kind)
    }

    private suspend fun enrichBandMembers(
        request: EnrichmentRequest.ForArtist,
        type: EnrichmentType,
    ): EnrichmentResult {
        return try {
            val artistId = api.searchArtist(request.name)
                ?: return EnrichmentResult.NotFound(type, id)
            val artist = api.getArtist(artistId)
                ?: return EnrichmentResult.NotFound(type, id)
            if (artist.members.isEmpty()) {
                return EnrichmentResult.NotFound(type, id)
            }
            success(DiscogsMapper.toBandMembers(artist), type)
        } catch (e: Exception) {
            mapError(type, e)
        }
    }

    private fun enrichFromRelease(
        release: DiscogsRelease,
        type: EnrichmentType,
    ): EnrichmentResult {
        val data = when (type) {
            EnrichmentType.ALBUM_ART -> DiscogsMapper.toArtwork(release)
            EnrichmentType.LABEL -> DiscogsMapper.toLabelMetadata(release)
            EnrichmentType.RELEASE_TYPE -> DiscogsMapper.toReleaseTypeMetadata(release)
            EnrichmentType.ALBUM_METADATA -> DiscogsMapper.toAlbumMetadata(release)
            else -> null
        } ?: return EnrichmentResult.NotFound(type, id)
        return success(data, type, release)
    }

    private fun buildResolvedIdentifiers(release: DiscogsRelease): EnrichmentIdentifiers? {
        var ids = EnrichmentIdentifiers()
        if (release.releaseId != null) {
            ids = ids.withExtra("discogsReleaseId", release.releaseId.toString())
        }
        if (release.masterId != null) {
            ids = ids.withExtra("discogsMasterId", release.masterId.toString())
        }
        return if (ids.extra.isEmpty()) null else ids
    }

    private fun success(
        data: EnrichmentData,
        type: EnrichmentType,
        release: DiscogsRelease? = null,
    ) = EnrichmentResult.Success(
        type = type,
        data = data,
        provider = id,
        confidence = ConfidenceCalculator.fuzzyMatch(hasArtistMatch = false),
        resolvedIdentifiers = release?.let { buildResolvedIdentifiers(it) },
    )
}
