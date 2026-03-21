---
phase: 03-public-api-cleanup
plan: 01
subsystem: api
tags: [enum, ttl, identifiers, extensibility, data-class, serialization]

# Dependency graph
requires:
  - phase: 02-provider-abstraction
    provides: mapper pattern, IdentifierRequirement, identity resolution
provides:
  - EnrichmentType enum with defaultTtlMs per entry
  - EnrichmentConfig.ttlOverrides for per-type TTL override
  - EnrichmentIdentifiers.extra map with get()/withExtra() for extensible identifiers
  - SimilarArtist and PopularTrack with EnrichmentIdentifiers instead of bare musicBrainzId
affects: [03-public-api-cleanup, 04-new-enrichment-types, 05-deepen-existing-types]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Enum-carried defaults with config overrides for TTL"
    - "Extensible identifier map (extra) on data classes for future provider IDs"
    - "EnrichmentIdentifiers as the canonical carrier for all entity IDs"

key-files:
  created:
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/EnrichmentIdentifiersTest.kt
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentConfig.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngine.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentRequest.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzMapper.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngineTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/testutil/FakeEnrichmentCache.kt

key-decisions:
  - "TTL values carried as defaultTtlMs constructor param on EnrichmentType enum entries, with config.ttlOverrides for per-type override"
  - "EnrichmentIdentifiers extra map uses immutable copy semantics via withExtra()"
  - "Added @Serializable to EnrichmentIdentifiers for use in @Serializable data classes (SimilarArtist, PopularTrack)"

patterns-established:
  - "Enum-carried defaults: configuration values belong on enum entries with config-level overrides"
  - "Extensible identifiers: new provider IDs added via extra map without data class changes"

requirements-completed: [API-01, API-02, API-03]

# Metrics
duration: 5min
completed: 2026-03-21
---

# Phase 03 Plan 01: Public API Type Cleanup Summary

**TTL moved into EnrichmentType enum entries with config overrides, extensible extra identifier map on EnrichmentIdentifiers, SimilarArtist/PopularTrack migrated to use EnrichmentIdentifiers**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T08:39:37Z
- **Completed:** 2026-03-21T08:44:28Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- EnrichmentType enum entries now carry defaultTtlMs directly (90d for artwork/lyrics, 30d for artist/bio, 365d for metadata, 7d for popularity)
- EnrichmentConfig.ttlOverrides allows per-type TTL override without touching enum or engine code
- DefaultEnrichmentEngine.ttlFor() companion function removed; cache.put uses config override or type default
- EnrichmentIdentifiers extended with extra: Map<String, String>, get(), and withExtra() for extensible provider IDs
- SimilarArtist and PopularTrack now carry EnrichmentIdentifiers instead of bare musicBrainzId: String?
- LastFmMapper and ListenBrainzMapper construct EnrichmentIdentifiers for downstream data classes
- FakeEnrichmentCache enhanced to capture storedTtls for TTL assertion tests

## Task Commits

Each task was committed atomically:

1. **Task 1: Move TTL into EnrichmentType enum, add ttlOverrides to config, remove ttlFor()** - `2a8c04e` (feat)
2. **Task 2: Add extensible extra map to EnrichmentIdentifiers, migrate SimilarArtist and PopularTrack** - `cbda960` (feat)

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt` - Added defaultTtlMs constructor param to all enum entries
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentConfig.kt` - Added ttlOverrides map field
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngine.kt` - Removed ttlFor(), use config override or type default, merge extra maps in resolveIdentity()
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentRequest.kt` - Added extra map, get(), withExtra() to EnrichmentIdentifiers; added @Serializable
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt` - SimilarArtist/PopularTrack use identifiers: EnrichmentIdentifiers
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapper.kt` - Construct EnrichmentIdentifiers for SimilarArtist
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzMapper.kt` - Construct EnrichmentIdentifiers for PopularTrack
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/EnrichmentIdentifiersTest.kt` - New test for withExtra/get/field preservation
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngineTest.kt` - TTL enum value tests, ttlOverrides engine tests
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProviderTest.kt` - Updated assertion to use identifiers.musicBrainzId
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProviderTest.kt` - Updated assertions to use identifiers.musicBrainzId
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/testutil/FakeEnrichmentCache.kt` - Added storedTtls tracking

## Decisions Made
- TTL values carried as defaultTtlMs constructor param on EnrichmentType enum entries (not a separate companion function) to keep type and its default TTL co-located
- EnrichmentIdentifiers extra map uses immutable copy semantics via withExtra() following existing Kotlin data class copy pattern
- Added @Serializable to EnrichmentIdentifiers since it's now embedded in @Serializable data classes (SimilarArtist, PopularTrack) -- without this, kotlinx.serialization would fail

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added @Serializable to EnrichmentIdentifiers**
- **Found during:** Task 2 (migrating SimilarArtist/PopularTrack)
- **Issue:** SimilarArtist and PopularTrack are @Serializable, and now embed EnrichmentIdentifiers which was not @Serializable -- this would cause compilation errors
- **Fix:** Added @Serializable annotation and kotlinx.serialization.Serializable import to EnrichmentIdentifiers in EnrichmentRequest.kt
- **Files modified:** musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentRequest.kt
- **Verification:** Full test suite passes
- **Committed in:** cbda960 (Task 2 commit)

**2. [Rule 1 - Bug] Updated ListenBrainzProviderTest assertions for musicBrainzId path change**
- **Found during:** Task 2 (migrating PopularTrack)
- **Issue:** Two test assertions in ListenBrainzProviderTest used topTracks[0].musicBrainzId which no longer exists after migration to identifiers field
- **Fix:** Updated to topTracks[0].identifiers.musicBrainzId
- **Files modified:** musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProviderTest.kt
- **Verification:** All ListenBrainz tests pass
- **Committed in:** cbda960 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes were necessary for compilation and test correctness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Public API types cleaned up; ready for Plan 03-02 (error categorization and HttpResult)
- TTL infrastructure ready for any new enrichment types added in Phase 04
- Extensible identifiers ready for new provider IDs (Spotify, Deezer, etc.) without data class changes

---
*Phase: 03-public-api-cleanup*
*Completed: 2026-03-21*

## Self-Check: PASSED
