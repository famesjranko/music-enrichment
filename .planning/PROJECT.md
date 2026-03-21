# musicmeta

## What This Is

A pure Kotlin/JVM music metadata enrichment library that aggregates data from 11 public APIs (MusicBrainz, Last.fm, Wikidata, Wikipedia, Cover Art Archive, Fanart.tv, Deezer, iTunes, Discogs, ListenBrainz, LRCLIB) into a unified pipeline with priority chains, circuit breakers, rate limiting, and identity resolution. 25 enrichment types across 6 categories: artwork (7 types with multi-size support), metadata (8 types), text (3 types), relationships (3 types), statistics (2 types), and links (2 types).

## Core Value

Consumers get comprehensive, accurate music metadata from a single `enrich()` call without knowing which APIs exist, how they authenticate, or how to correlate identifiers across services.

## Current State

Shipped v0.4.0 with 12,261 LOC Kotlin. 25 enrichment types across 11 providers. All providers use typed identifier requirements, formalized identity resolution, mapper pattern, and standardized confidence scoring.

## Requirements

### Validated

- v0.1.0: 11 providers, 16 enrichment types, priority chains, circuit breakers, rate limiting
- v0.1.0: Identity resolution pipeline (MusicBrainz -> MBID/Wikidata/Wikipedia)
- v0.1.0: Fan-out concurrency, confidence filtering, configurable priorities
- v0.1.0: Pure Kotlin/JVM core + optional Android module (Room, Hilt, WorkManager)
- v0.4.0: 5 provider bugs fixed (MusicBrainz, Last.fm, LRCLIB, Wikidata)
- v0.4.0: Typed IdentifierRequirement enum, identity provider formalization, mapper pattern, ApiKeyConfig
- v0.4.0: TTL in EnrichmentType enum, extensible identifiers, ErrorKind, HttpResult
- v0.4.0: 6 new enrichment types (BAND_MEMBERS, ARTIST_DISCOGRAPHY, ALBUM_TRACKS, SIMILAR_TRACKS, ARTIST_BANNER, ARTIST_LINKS)
- v0.4.0: Artwork sizes, back cover/booklet art, album metadata deepening
- v0.4.0: Track-level popularity fix, ListenBrainz batch endpoints, ConfidenceCalculator

### Active

(None — planning next milestone)

### Out of Scope

- New providers (Spotify, Apple Music, etc.) — focus on extracting more from existing 11
- Android module changes — core-only focus
- CREDITS type — high effort, deferred (PRD Phase 3A)
- RELEASE_EDITIONS type — medium effort, deferred (PRD Phase 3B)
- Artist timeline — depends on credits/editions (PRD Phase 3C)
- Genre deep dive — deferred (PRD Phase 3D)
- Wikipedia structured HTML parsing — high complexity, low ROI vs Wikidata

## Context

- Pre-1.0 with no external consumers — clean breaking changes still safe
- Provider APIs are the biggest long-term maintenance risk — now mitigated by mapper pattern
- v0.4.0 tech debt: ErrorKind/HttpResult exist but not yet adopted by providers; Deezer SIMILAR_TRACKS not implemented; ListenBrainz ARTIST_DISCOGRAPHY plumbing exists but not wired as capability
- 328 tests passing (unit + serialization)

## Constraints

- **Stack**: Pure Kotlin/JVM, Java 17, org.json for parsing, kotlinx.serialization
- **Style**: 200-line files (300 max), 20-line functions (40 max), no `!!`
- **Testing**: Fakes over mocks, runTest for unit tests, runBlocking for E2E
- **API compliance**: MusicBrainz 1 req/sec, descriptive User-Agent for Wikimedia APIs
- **Dependencies**: Managed via gradle/libs.versions.toml version catalog

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| MusicBrainz as identity backbone | MBIDs + Wikidata/Wikipedia links enable precise downstream lookups | ✓ Good |
| Provider mapper pattern | Isolate provider code from public API shape; changes to EnrichmentData only touch mappers | ✓ Good — 11 mappers, zero inline construction |
| Clean breaks over deprecation | No external consumers at v0.1.0; deprecation adds complexity for zero benefit | ✓ Good — IdentifierResolution cleanly removed |
| Remove IdentifierResolution from public API | Internal concept leaked into sealed class; identity resolution is engine-internal | ✓ Good — replaced by resolvedIdentifiers on Success |
| Typed IdentifierRequirement enum | Boolean `requiresIdentifier` too coarse; providers need MUSICBRAINZ_ID vs WIKIDATA_ID | ✓ Good — 6 enum values, precise chain filtering |
| ConfidenceCalculator utility | Standardize confidence scoring across providers without enforcement | ✓ Good — all 11 providers adopted, zero hardcoded floats |

---
*Last updated: 2026-03-21 after v0.4.0 milestone*
