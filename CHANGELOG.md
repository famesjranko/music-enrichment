# Changelog

All notable changes to musicmeta will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Enrichment showcase test (`EnrichmentShowcaseTest`) — comprehensive E2E diagnostic that exercises all 16 enrichment types across artists, albums, and tracks with formatted output showing provider coverage, gaps, and a wishlist of unimplemented features
- API key forwarding in `build.gradle.kts` — Last.fm, Fanart.tv, and Discogs keys can be passed via system properties (`-Dlastfm.apikey=KEY`) or environment variables (`LASTFM_API_KEY`)

### Fixed
- Artist identity resolution now resolves wikidata/wikipedia URLs — MusicBrainz was skipping the full lookup during identity resolution when the requested type was GENRE/LABEL/etc., causing ARTIST_PHOTO and ARTIST_BIO to always fail for artist requests
- E2E tests switched from `runTest` to `runBlocking` — `runTest`'s virtual time caused `withTimeout` in the engine to fire prematurely, silently returning empty results from every `enrich()` call
- `extractResolution` in `RealApiEndToEndTest` updated to match current engine data model (engine stores `Metadata` + `resolvedIdentifiers`, not `IdentifierResolution`)

### Changed
- Provider priorities now configurable via `EnrichmentConfig.priorityOverrides` — reorder provider chains without modifying provider code
- MusicBrainz minimum match score now a constructor param (`minMatchScore`, default 80)
- Artwork sizes now per-provider constructor params: `CoverArtArchiveProvider(artworkSize, thumbnailSize)`, `ITunesProvider(artworkSize)`, `WikidataProvider(imageSize)`, `MusicBrainzProvider(thumbnailSize)`
- `ArtistMatcher.isMatch()` accepts optional `minTokenOverlap` parameter (default 0.5)
- Room database name extracted to `HiltEnrichmentModule.DEFAULT_DATABASE_NAME`
- Removed `preferredArtworkSize` and `thumbnailSize` from `EnrichmentConfig` — artwork sizes are per-provider constructor concerns, not engine-level config

### Fixed
- Silent exception swallowing in `DefaultEnrichmentEngine` — identity search, supplemental search, and identity resolution failures now log through `EnrichmentLogger`
- Silent deserialization failure in `RoomEnrichmentCache.get()` now logs through `EnrichmentLogger`
- Cascade-specific User-Agent strings in E2E tests replaced with generic `MusicMetaTest/1.0`
- Stale `enrichment-core` module name in E2E test comments updated to `musicmeta-core`

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
- `musicmeta-android` module: `RoomEnrichmentCache`, `HiltEnrichmentModule`, `EnrichmentWorker`
- E2E test suite against real APIs (gated by `-Dinclude.e2e=true`)
- JitPack publishing support
