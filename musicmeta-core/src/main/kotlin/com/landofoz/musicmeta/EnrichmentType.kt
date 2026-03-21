package com.landofoz.musicmeta

/**
 * Types of data that can be enriched for a music entity.
 *
 * @param defaultTtlMs Default cache time-to-live in milliseconds for this type.
 *   Can be overridden per-type via [EnrichmentConfig.ttlOverrides].
 */
enum class EnrichmentType(val defaultTtlMs: Long) {
    // Artwork — 90 days
    ALBUM_ART(90L * 24 * 60 * 60 * 1000),
    ARTIST_PHOTO(30L * 24 * 60 * 60 * 1000),
    ARTIST_BACKGROUND(30L * 24 * 60 * 60 * 1000),
    ARTIST_LOGO(90L * 24 * 60 * 60 * 1000),
    CD_ART(90L * 24 * 60 * 60 * 1000),

    // Structured metadata
    GENRE(90L * 24 * 60 * 60 * 1000),
    LABEL(365L * 24 * 60 * 60 * 1000),
    RELEASE_DATE(365L * 24 * 60 * 60 * 1000),
    RELEASE_TYPE(365L * 24 * 60 * 60 * 1000),
    COUNTRY(365L * 24 * 60 * 60 * 1000),
    SIMILAR_ARTISTS(30L * 24 * 60 * 60 * 1000),

    // Text content
    ARTIST_BIO(30L * 24 * 60 * 60 * 1000),
    LYRICS_SYNCED(90L * 24 * 60 * 60 * 1000),
    LYRICS_PLAIN(90L * 24 * 60 * 60 * 1000),

    // Statistics — 7 days
    TRACK_POPULARITY(7L * 24 * 60 * 60 * 1000),
    ARTIST_POPULARITY(7L * 24 * 60 * 60 * 1000),
}
