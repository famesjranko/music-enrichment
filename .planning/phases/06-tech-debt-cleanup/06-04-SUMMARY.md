---
phase: 06-tech-debt-cleanup
plan: "04"
subsystem: provider
tags: [listenbrainz, discogs, enrichment, identifiers]

requires:
  - phase: 06-03
    provides: HttpResult migration for all providers

provides:
  - ListenBrainz ARTIST_DISCOGRAPHY capability at priority 50 with MUSICBRAINZ_ID requirement
  - Discogs release ID and master ID parsed from search JSON and stored in resolvedIdentifiers

affects:
  - 07-credits (uses discogsReleaseId/discogsMasterId for precise Discogs lookups)
  - 08-release-editions (uses discogsMasterId for master release endpoint)
  - 09-artist-timeline (uses ListenBrainz ARTIST_DISCOGRAPHY for release group data)

tech-stack:
  added: []
  patterns:
    - "resolvedIdentifiers pattern: providers store discovered IDs in EnrichmentResult.Success.resolvedIdentifiers via extra map"
    - "Default parameter pattern: success() accepts optional DiscogsRelease to populate IDs without breaking band members call site"

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProviderTest.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt

key-decisions:
  - "Discogs IDs stored via extra map (discogsReleaseId, discogsMasterId) on EnrichmentIdentifiers - consistent with existing withExtra pattern"
  - "masterId omitted from resolvedIdentifiers when master_id is 0 - 0 means no master exists in Discogs API"
  - "enrichDiscography wraps exception handling in try/catch matching existing enrichArtistPopularity pattern"

patterns-established:
  - "buildResolvedIdentifiers helper: converts model IDs to EnrichmentIdentifiers extra map, returns null if both absent"

requirements-completed: [DEBT-03, DEBT-04]

duration: 15min
completed: 2026-03-22
---

# Phase 06 Plan 04: Identifier Gaps Closed Summary

**ListenBrainz ARTIST_DISCOGRAPHY wired at priority 50 and Discogs release/master IDs stored in resolvedIdentifiers via discogsReleaseId/discogsMasterId extra map keys**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-03-22T00:00:00Z
- **Completed:** 2026-03-22T00:15:00Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- ListenBrainz now exposes ARTIST_DISCOGRAPHY capability using existing getTopReleaseGroupsForArtist + toDiscography plumbing that was implemented in Phase 5 but never wired
- Discogs search results now carry releaseId and masterId from the "id" and "master_id" JSON fields, stored on resolvedIdentifiers for downstream providers to use for precise lookups
- 7 new TDD tests added (4 for ListenBrainz, 3 for Discogs)

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire ListenBrainz ARTIST_DISCOGRAPHY capability** - `4d4cf02` (feat)
2. **Task 2: Store Discogs release ID and master ID from search results** - `7da3e6a` (feat)

_Note: TDD tasks - tests written first (RED), then implementation (GREEN in same commit)_

## Files Created/Modified

- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProvider.kt` - Added ARTIST_DISCOGRAPHY capability, routing, and enrichDiscography method
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProviderTest.kt` - Added 4 tests for discography: success, empty, no MBID, capability check
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt` - Added releaseId and masterId fields to DiscogsRelease
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt` - Parses "id" and "master_id" from search result JSON
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt` - Added buildResolvedIdentifiers, updated success() with optional release param
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt` - Added 3 tests: both IDs stored, only releaseId when masterId=0, null when no IDs

## Decisions Made

- Discogs IDs stored via `extra` map keys `discogsReleaseId` and `discogsMasterId` — consistent with the extensible `withExtra` pattern already established on `EnrichmentIdentifiers`
- `masterId` omitted from `resolvedIdentifiers` when `master_id` is 0 — the Discogs API uses 0 to indicate no master exists, so `takeIf { it > 0 }` correctly treats it as absent
- `enrichDiscography` wraps in try/catch matching the existing `enrichArtistPopularity` pattern — consistent error handling across all ListenBrainz enrichment methods

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 6 tech debt cleanup complete: DEBT-01 through DEBT-04 all resolved
- Phase 7 (Credits) can now use `discogsReleaseId`/`discogsMasterId` from resolvedIdentifiers for precise Discogs release endpoint lookups
- Phase 8 (Release Editions) can use `discogsMasterId` for the Discogs master release endpoint
- Phase 9 (Artist Timeline) can use ListenBrainz ARTIST_DISCOGRAPHY at priority 50 via the provider chain

---
*Phase: 06-tech-debt-cleanup*
*Completed: 2026-03-22*
