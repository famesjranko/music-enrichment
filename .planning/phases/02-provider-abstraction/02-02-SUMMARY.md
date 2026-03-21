---
phase: 02-provider-abstraction
plan: 02
subsystem: api
tags: [identity-resolution, sealed-class, enrichment-data, provider-abstraction]

# Dependency graph
requires:
  - phase: 02-provider-abstraction (plan 01)
    provides: "isIdentityProvider flag, resolveIdentity() interface method, IdentifierRequirement enum"
provides:
  - "EnrichmentData sealed class without IdentifierResolution subclass"
  - "MusicBrainzProvider returning Metadata + resolvedIdentifiers on Success"
  - "DefaultEnrichmentEngine.resolveIdentity() using provider.resolveIdentity() and result.resolvedIdentifiers"
  - "FakeProvider.givenIdentityResult() for testing identity resolution"
affects: [02-provider-abstraction, public-api-cleanup]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Identity resolution returns Metadata data with EnrichmentIdentifiers on Success.resolvedIdentifiers"
    - "Engine reads resolved IDs from result.resolvedIdentifiers instead of downcasting data"

key-files:
  created: []
  modified:
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngine.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/testutil/FakeProvider.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProviderTest.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngineTest.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/RealApiEndToEndTest.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/ProviderValidationTest.kt"

key-decisions:
  - "IdentifierResolution removed as clean break (no deprecation) since pre-1.0 has no external consumers"
  - "Engine calls provider.resolveIdentity() instead of provider.enrich() for identity resolution, completing the formalized identity provider role"

patterns-established:
  - "Identity data pattern: providers return EnrichmentData.Metadata with resolved IDs on EnrichmentResult.Success.resolvedIdentifiers"
  - "FakeProvider identity testing: use givenIdentityResult() to configure identity resolution responses"

requirements-completed: [ABST-03]

# Metrics
duration: 5min
completed: 2026-03-21
---

# Phase 02 Plan 02: Remove IdentifierResolution Summary

**Removed IdentifierResolution sealed subclass from public API; MusicBrainzProvider returns Metadata directly with resolved IDs on Success.resolvedIdentifiers, engine uses provider.resolveIdentity() for the identity resolution pathway**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T08:11:24Z
- **Completed:** 2026-03-21T08:17:11Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Deleted EnrichmentData.IdentifierResolution sealed subclass, removing internal concept from public API
- Reworked MusicBrainzProvider to return EnrichmentData.Metadata directly with EnrichmentIdentifiers attached via resolvedIdentifiers
- Rewrote DefaultEnrichmentEngine.resolveIdentity() to use the formalized provider.resolveIdentity() method and read IDs from result.resolvedIdentifiers
- Updated all unit tests and E2E tests to use new data shape (zero IdentifierResolution references remain in codebase)

## Task Commits

Each task was committed atomically:

1. **Task 1: Rework MusicBrainzProvider** - `c53b27f` (feat)
2. **Task 2: Rework engine and remove IdentifierResolution** - `99e3868` (feat)

## Files Created/Modified
- `EnrichmentData.kt` - Removed IdentifierResolution sealed subclass (11 lines deleted)
- `EnrichmentProvider.kt` - Updated KDoc to reference Metadata instead of IdentifierResolution
- `MusicBrainzProvider.kt` - Replaced buildAlbumResolution/buildArtistResolution with buildAlbumResult/buildArtistResult returning Success with Metadata + resolvedIdentifiers
- `DefaultEnrichmentEngine.kt` - Rewrote resolveIdentity() to call provider.resolveIdentity() and read result.resolvedIdentifiers
- `FakeProvider.kt` - Added givenIdentityResult() and resolveIdentity() override for identity resolution testing
- `MusicBrainzProviderTest.kt` - Updated assertions to check resolvedIdentifiers on Success instead of IdentifierResolution fields
- `DefaultEnrichmentEngineTest.kt` - Rewrote 4 identity resolution tests to use Metadata + resolvedIdentifiers pattern
- `RealApiEndToEndTest.kt` - Replaced extractResolution helper with ResolvedInfo data class (no IdentifierResolution dependency)
- `EnrichmentShowcaseTest.kt` - Updated printResults to read resolvedIdentifiers from Success; removed IdentifierResolution branch from snippet()
- `ProviderValidationTest.kt` - Changed all IdentifierResolution casts to Metadata + resolvedIdentifiers assertions

## Decisions Made
- **Clean break, no deprecation**: IdentifierResolution deleted outright since pre-1.0 has no external consumers. No adapter or migration path needed.
- **Engine uses provider.resolveIdentity()**: Completes the formalized identity provider role from Plan 01. The engine no longer calls `enrich()` for identity resolution, making the contract explicit.
- **E2E helper uses local data class**: Rather than keeping IdentifierResolution for test convenience, created a lightweight `ResolvedInfo` data class local to RealApiEndToEndTest for the same extraction pattern.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated EnrichmentProvider KDoc comment**
- **Found during:** Task 2
- **Issue:** KDoc on `resolveIdentity()` still referenced "IdentifierResolution data"
- **Fix:** Changed to "Metadata data and resolvedIdentifiers"
- **Files modified:** EnrichmentProvider.kt
- **Committed in:** 99e3868 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Minor documentation fix. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- IdentifierResolution fully removed; public API surface is cleaner
- Plan 03 (mapper pattern) can now proceed without IdentifierResolution coupling
- All production code uses the Metadata + resolvedIdentifiers pattern consistently

## Self-Check: PASSED

- All key files exist (5/5 verified)
- All commits exist (c53b27f, 99e3868)
- Zero IdentifierResolution references in production Kotlin code
- Full test suite passes

---
*Phase: 02-provider-abstraction*
*Completed: 2026-03-21*
