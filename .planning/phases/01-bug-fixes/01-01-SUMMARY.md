---
phase: 01-bug-fixes
plan: 01
subsystem: providers
tags: [musicbrainz, lastfm, bugfix, https, enrichment-result]

# Dependency graph
requires: []
provides:
  - MusicBrainz empty-result returns NotFound (not RateLimited)
  - Last.fm API calls use HTTPS
  - Last.fm TRACK_POPULARITY capability removed
affects: [02-provider-abstraction, 03-public-api]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProviderTest.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProviderTest.kt

key-decisions:
  - "Null API response and empty results both map to NotFound (API layer conflates both to emptyList)"

patterns-established: []

requirements-completed: [BUG-01, BUG-02, BUG-03]

# Metrics
duration: 4min
completed: 2026-03-21
---

# Phase 01 Plan 01: Provider Bug Fixes Summary

**Fix MusicBrainz empty-result misclassification to NotFound, Last.fm HTTP-to-HTTPS, and TRACK_POPULARITY removal with 6 new TDD tests**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-21T07:19:30Z
- **Completed:** 2026-03-21T07:23:38Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- MusicBrainz empty search results now return NotFound instead of RateLimited for all three request types (album, artist, track)
- Last.fm API traffic switched from HTTP to HTTPS
- TRACK_POPULARITY removed from Last.fm capabilities and enrich routing (4 capabilities remain: SIMILAR_ARTISTS, GENRE, ARTIST_BIO, ARTIST_POPULARITY)
- 6 new unit tests added across both providers, all passing

## Task Commits

Each task was committed atomically (TDD: RED then GREEN):

1. **Task 1: Fix MusicBrainz empty results returning RateLimited (BUG-01)**
   - `26ab76d` (test) RED: add failing tests for empty-result NotFound
   - `698a6a9` (fix) GREEN: change all three empty-result branches to NotFound

2. **Task 2: Fix Last.fm HTTPS and remove TRACK_POPULARITY (BUG-02, BUG-03)**
   - `ff65bd9` (test) RED: add failing tests for HTTPS and TRACK_POPULARITY
   - `5064511` (fix) GREEN: HTTPS base URL, remove TRACK_POPULARITY capability and routing

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt` - Changed 3 empty-result returns from RateLimited to NotFound
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProviderTest.kt` - Added 3 empty-result tests, renamed null-response test
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmApi.kt` - Changed BASE_URL to HTTPS
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProvider.kt` - Removed TRACK_POPULARITY from capabilities and when-block
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProviderTest.kt` - Added 3 tests for HTTPS, capability, and NotFound

## Decisions Made
- Null API response (no canned response in FakeHttpClient) and empty results (empty JSON array) both correctly map to NotFound because the API layer conflates both cases to emptyList(). The existing test was renamed from `enrich returns RateLimited on null response` to `enrich returns NotFound on null response` to reflect this.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All three provider bugs (BUG-01, BUG-02, BUG-03) fixed and verified
- Provider code ready for abstraction work in Phase 02

## Self-Check: PASSED

All 6 files verified present. All 4 commit hashes (26ab76d, 698a6a9, ff65bd9, 5064511) found in git log.

---
*Phase: 01-bug-fixes*
*Completed: 2026-03-21*
