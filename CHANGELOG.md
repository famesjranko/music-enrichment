# Changelog

All notable changes to musicmeta will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
