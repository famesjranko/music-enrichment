---
phase: 02-provider-abstraction
plan: 03
subsystem: api
tags: [mapper-pattern, api-key-config, provider-abstraction, builder-pattern]

# Dependency graph
requires:
  - phase: 02-provider-abstraction (plan 02)
    provides: "IdentifierResolution removed, Metadata + resolvedIdentifiers pattern"
provides:
  - "11 *Mapper.kt objects isolating DTO-to-EnrichmentData mapping per provider"
  - "ApiKeyConfig data class for centralized API key management"
  - "Builder.apiKeys() and Builder.withDefaultProviders() for one-line engine setup"
affects: [public-api-cleanup, new-enrichment-types]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Mapper pattern: each provider has a companion *Mapper.kt object with pure functions converting DTOs to EnrichmentData"
    - "Builder convenience: withDefaultProviders() creates all providers with correct rate limiters and API keys"

key-files:
  created:
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/coverartarchive/CoverArtArchiveMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibMapper.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/BuilderDefaultProvidersTest.kt"
  modified:
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentConfig.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentEngine.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/coverartarchive/CoverArtArchiveProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibProvider.kt"

key-decisions:
  - "Mapper objects use pure functions (no state), same package as provider for natural visibility"
  - "withDefaultProviders() creates separate RateLimiter instances: 1100ms for MusicBrainz, 100ms default"
  - "ApiKeyConfig uses nullable String fields (null = key not provided, provider skipped)"

patterns-established:
  - "Mapper pattern: object XMapper { fun toY(dto): EnrichmentData.Y } in provider package"
  - "Provider delegates all EnrichmentData construction to its mapper"
  - "Builder.withDefaultProviders() for zero-config engine setup"

requirements-completed: [ABST-04, ABST-05]

# Metrics
duration: 7min
completed: 2026-03-21
---

# Phase 02 Plan 03: Mapper Pattern and API Key Config Summary

**Extracted 11 mapper objects isolating DTO-to-EnrichmentData mapping from provider logic, plus ApiKeyConfig with Builder.withDefaultProviders() for one-line engine setup**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-21T08:19:17Z
- **Completed:** 2026-03-21T08:26:20Z
- **Tasks:** 2
- **Files modified:** 24 (13 created, 11 modified)

## Accomplishments
- Created 11 *Mapper.kt objects, one per provider, isolating all EnrichmentData construction from provider logic
- Updated all 11 providers to delegate EnrichmentData construction to their mapper (zero inline construction remains)
- Added ApiKeyConfig data class with lastFmKey, fanartTvProjectKey, discogsPersonalToken
- Added Builder.apiKeys() and Builder.withDefaultProviders() for centralized provider setup
- TDD tests verify: 8 providers without keys, 11 with all keys, 9 with single key

## Task Commits

Each task was committed atomically:

1. **Task 1: Extract 11 mapper files from providers** - `0754f52` (feat)
2. **Task 2: Add ApiKeyConfig and Builder.withDefaultProviders()** - `5098b00` (test: RED), `4a8ac3b` (feat: GREEN)

## Files Created/Modified
- `provider/*/XMapper.kt` (11 files) - Pure mapping functions from DTOs to EnrichmentData subclasses
- `provider/*/XProvider.kt` (11 files) - Updated to delegate to mappers
- `EnrichmentConfig.kt` - Added ApiKeyConfig data class
- `EnrichmentEngine.kt` - Added apiKeys(), withDefaultProviders() to Builder
- `engine/BuilderDefaultProvidersTest.kt` - 4 tests for Builder convenience methods

## Decisions Made
- **Mapper objects use pure functions, same package as provider**: No `internal` modifier needed since they share a package. Functions are naturally scoped to the provider they belong to.
- **withDefaultProviders creates separate RateLimiter instances**: MusicBrainz gets 1100ms (API requirement), all others get 100ms default. Each provider gets its own limiter to avoid cross-provider throttling.
- **ApiKeyConfig uses nullable Strings**: Null means key not provided and the corresponding provider is not registered. This is simpler than empty-string checks and more explicit.

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 02 Provider Abstraction is fully complete (all 3 plans done)
- Mapper pattern established for all providers, enabling safe public API shape changes
- ApiKeyConfig and withDefaultProviders() simplify consumer setup
- Ready to proceed to Phase 03 (Public API Cleanup)

## Self-Check: PASSED

- All 14 key files exist (14/14 verified)
- All commits exist (0754f52, 5098b00, 4a8ac3b)
- Zero inline EnrichmentData construction in provider files
- Full test suite passes

---
*Phase: 02-provider-abstraction*
*Completed: 2026-03-21*
