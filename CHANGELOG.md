# Changelog

All notable changes to musicmeta will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.4.0] - 2026-03-21

Provider Abstraction Overhaul — 5 phases, 15 plans, 25 enrichment types, 328 tests.

### Added
- 9 new enrichment types: `BAND_MEMBERS`, `ARTIST_DISCOGRAPHY`, `ALBUM_TRACKS`, `SIMILAR_TRACKS`, `ARTIST_BANNER`, `ARTIST_LINKS`, `ALBUM_ART_BACK`, `ALBUM_BOOKLET`, `ALBUM_METADATA`
- `ArtworkSize` data class and `Artwork.sizes` field — multi-size artwork support across Cover Art Archive, Deezer, iTunes, and Fanart.tv
- `IdentifierRequirement` enum replacing boolean `requiresIdentifier` — typed identifier checking per provider (MUSICBRAINZ_ID, WIKIDATA_ID, WIKIPEDIA_TITLE, etc.)
- `isIdentityProvider` flag and `resolveIdentity()` method on `EnrichmentProvider` interface — formalized identity resolution as a provider role
- 11 `*Mapper.kt` files — provider mapper pattern isolating DTO-to-EnrichmentData mapping from provider logic
- `ApiKeyConfig` data class and `EnrichmentEngine.Builder.apiKeys()` + `withDefaultProviders()` — one-line engine setup
- `EnrichmentIdentifiers.extra` map with `get()` and `withExtra()` — extensible identifier storage for provider-specific IDs (deezerId, discogsArtistId)
- `ErrorKind` enum on `EnrichmentResult.Error` — categorize errors as NETWORK, AUTH, PARSE, RATE_LIMIT, UNKNOWN
- `HttpResult` sealed class with `fetchJsonResult()` on `HttpClient` — typed HTTP responses (Ok, ClientError, ServerError, RateLimited, NetworkError)
- `ConfidenceCalculator` utility — standardized confidence scoring (idBasedLookup, authoritative, searchScore, fuzzyMatch) across all 11 providers
- `EnrichmentType.defaultTtlMs` — TTL moved into enum with `EnrichmentConfig.ttlOverrides` for per-type override
- MusicBrainz: band members via artist-rels, discography via release-group browse, tracklist from media array, artist links from all URL relation types
- Deezer: artist discography via `/artist/{id}/albums`, album tracks via `/album/{id}/tracks`, album metadata (trackCount, explicit, genres)
- Last.fm: `track.getSimilar` for SIMILAR_TRACKS, `track.getInfo` for track-level TRACK_POPULARITY (replacing artist-level data)
- Fanart.tv: ARTIST_BANNER capability via musicbanner images
- Cover Art Archive: JSON metadata endpoint for back cover and booklet art with image type filtering
- Discogs: band members via artist endpoint, album metadata (catalogNumber, communityRating)
- iTunes: album metadata (trackCount, primaryGenreName)
- ListenBrainz: batch POST endpoints for recording and artist popularity, top release groups for artist
- Wikidata: expanded properties — P569 (birth date), P570 (death date), P495 (country of origin), P106 (occupation) in a single API call
- Wikipedia: ARTIST_PHOTO via page media-list endpoint as supplemental source
- Enrichment showcase test updated to reflect v0.4.0 coverage (25 types)

### Changed
- `ProviderCapability.requiresIdentifier: Boolean` replaced by `identifierRequirement: IdentifierRequirement` enum
- `ProviderRegistry.identityProvider()` selects by `isIdentityProvider` flag instead of GENRE/LABEL heuristic
- `DefaultEnrichmentEngine.needsIdentityResolution()` is data-driven from provider capabilities, not hardcoded type list
- `DefaultEnrichmentEngine.ttlFor()` removed — TTL now on `EnrichmentType.defaultTtlMs` with config override
- `SimilarArtist.musicBrainzId: String?` replaced by `identifiers: EnrichmentIdentifiers`
- `PopularTrack.musicBrainzId: String?` replaced by `identifiers: EnrichmentIdentifiers`
- `EnrichmentData.IdentifierResolution` removed from public sealed class — identity resolution uses `resolvedIdentifiers` on `EnrichmentResult.Success`
- MusicBrainzProvider returns `Metadata` directly from identity resolution instead of `IdentifierResolution`
- All 11 providers delegate EnrichmentData construction to mapper objects (zero inline construction)
- All 11 providers use `ConfidenceCalculator` methods (zero hardcoded float confidence values)

### Fixed
- MusicBrainz: empty search results return `NotFound` instead of `RateLimited` (3 locations)
- Last.fm: API base URL uses HTTPS instead of HTTP
- Last.fm: `TRACK_POPULARITY` removed from capabilities (was returning artist-level data); properly restored with `track.getInfo`
- LRCLIB: duration parameter uses `Double` instead of `Int` — preserves fractional seconds (238500ms → 238.5s, not 238s)
- Wikidata: claim resolution filters for preferred-rank claims before falling back to first in array
- Wikidata: URL-encode pipe characters in multi-property query string (prevents `URISyntaxException`)

## [0.1.0] - 2026-03-21

### Added
- `EnrichmentEngine` with builder pattern, fan-out provider chains, confidence filtering, and configurable timeout
- Identity resolution pipeline — MusicBrainz resolves MBIDs, Wikidata IDs, and Wikipedia titles for downstream providers
- 11 providers: MusicBrainz, Cover Art Archive, Wikidata, Wikipedia, LRCLIB, Deezer, iTunes, Last.fm, ListenBrainz, Fanart.tv, Discogs
- `ProviderChain` with priority ordering and circuit breakers per provider
- `RateLimiter` for per-provider request throttling
- `InMemoryEnrichmentCache` with LRU eviction and TTL
- `EnrichmentConfig` with `minConfidence`, `confidenceOverrides`, `enableIdentityResolution`, `enrichTimeoutMs`
- `HttpClient` interface with `DefaultHttpClient` (java.net.HttpURLConnection)
- `ArtistMatcher` for music-aware fuzzy name matching across providers
- Search API (`engine.search()`) with candidate deduplication across providers
- Enrichment showcase test (`EnrichmentShowcaseTest`) — comprehensive E2E diagnostic
- API key forwarding in `build.gradle.kts` — Last.fm, Fanart.tv, and Discogs keys via system properties or env vars
- `musicmeta-android` module: `RoomEnrichmentCache`, `HiltEnrichmentModule`, `EnrichmentWorker`
- E2E test suite against real APIs (gated by `-Dinclude.e2e=true`)
- JitPack publishing support

### Fixed
- Artist identity resolution resolves wikidata/wikipedia URLs during all identity lookups
- E2E tests use `runBlocking` (not `runTest`) to avoid virtual-time timeout issues
- Silent exception swallowing in engine and cache now logged through `EnrichmentLogger`

### Changed
- Provider priorities configurable via `EnrichmentConfig.priorityOverrides`
- MusicBrainz minimum match score is a constructor param (`minMatchScore`, default 80)
- Artwork sizes are per-provider constructor params (not engine-level config)
