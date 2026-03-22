---
phase: 07-credits-personnel
plan: "02"
subsystem: provider
tags: [discogs, credits, kotlin, tdd]

# Dependency graph
requires:
  - phase: 07-credits-personnel/07-01
    provides: Credits and Credit @Serializable data classes with roleCategory field
  - phase: 06-tech-debt-cleanup
    provides: discogsReleaseId stored in extra map by DEBT-04; HttpResult/ErrorKind patterns

provides:
  - DiscogsReleaseDetail, DiscogsCredit, DiscogsTrackItem models
  - getReleaseDetails API method fetching extraartists and tracklist from Discogs releases endpoint
  - DiscogsMapper.toCredits and mapRoleCategory (performance/production/songwriting/null)
  - Discogs CREDITS capability at priority 50 (fallback behind MusicBrainz at 100)
  - enrichTrackCredits: reads discogsReleaseId from identifiers.extra, prefers track-level credits

affects:
  - consumers using ForTrack + CREDITS enrichment (now get credits from both MusicBrainz and Discogs)
  - future phases extending Discogs provider with additional track-level enrichment

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "DiscogsCredit id field uses takeIf { it > 0 } (not null) to map Discogs 0-means-absent convention"
    - "parseCreditsArray private helper shared by release-level and track-level extraartists parsing"
    - "CREDITS dispatch guard at top of enrich() before album-type check, consistent with BAND_MEMBERS pattern"

key-files:
  created:
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapperTest.kt
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt

key-decisions:
  - "DiscogsCredit id uses Long? with takeIf { it > 0 } — Discogs API returns 0 when artist ID absent (same convention as masterId in Phase 6)"
  - "parseCreditsArray extracted as private helper to parse both release.extraartists and track.extraartists without duplication"
  - "enrichTrackCredits placed before album-type check in enrich(), consistent with BAND_MEMBERS dispatch pattern at the top of the method"

patterns-established:
  - "mapRoleCategory: keyword-based lowercase contains() matching maps Discogs free-text roles to semantic categories"

requirements-completed: [CRED-03, CRED-04]

# Metrics
duration: 20min
completed: 2026-03-22
---

# Phase 07 Plan 02: Discogs Credits Provider Summary

**Discogs CREDITS fallback at priority 50 using discogsReleaseId from Phase 6 to fetch release extraartists, with track-level filtering and role-to-category keyword mapping covering performance/production/songwriting**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-03-22T16:45:00Z
- **Completed:** 2026-03-22T17:05:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- DiscogsReleaseDetail, DiscogsCredit, DiscogsTrackItem models added to DiscogsModels.kt
- getReleaseDetails API method with extraartists and per-track extraartists parsing
- DiscogsMapper.toCredits + mapRoleCategory helper with 27 keyword matchers (vocal/guitar/bass/drum/keyboard/piano/percussion/instrument/perform/featuring/orchestra/choir/strings → performance; produc/engineer/mix/master/record/remix/program → production; written/writer/compos/lyric/arrang/music by/words by → songwriting)
- Discogs CREDITS capability at priority 50 wired into DiscogsProvider; enrichTrackCredits prefers track-level extraartists, falls back to release-level

## Task Commits

Each task was committed atomically:

1. **Task 1: Discogs release details API + credit models + mapper** - `4740f64` (feat)
2. **Task 2: Wire Discogs CREDITS capability into Provider** - `09f771d` (feat)

## Files Created/Modified

- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt` - Added DiscogsReleaseDetail, DiscogsCredit, DiscogsTrackItem data classes
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt` - Added getReleaseDetails, parseReleaseDetail, parseCreditsArray, RELEASES_URL constant
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt` - Added toCredits and mapRoleCategory functions
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt` - Added CREDITS capability, enrichTrackCredits, CREDITS dispatch guard
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapperTest.kt` - Created with 14 tests covering mapRoleCategory and toCredits
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt` - Added 6 CREDITS tests (track match, release fallback, no releaseId, IOException, no credits, capability check)

## Decisions Made

- `DiscogsCredit.id` uses `Long?` with `takeIf { it > 0 }` pattern — Discogs API uses 0 for absent artist IDs, same convention as `masterId` established in Phase 6.
- `parseCreditsArray` extracted as private helper shared by release-level and track-level extraartists parsing — avoids duplicating a 10-line loop.
- CREDITS dispatch guard placed before the album-type cast in `enrich()`, consistent with how `BAND_MEMBERS` guard sits at the top of the method.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all TDD phases (RED/GREEN) executed as expected.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- CREDITS type now has two providers: MusicBrainz (priority 100) + Discogs (priority 50 fallback)
- Phase 8 (Release Editions) can start independently — same discogsReleaseId identifier is available
- All existing tests still pass

## Known Stubs

None - Credits data wired from Discogs API via getReleaseDetails with extraartists and tracklist.

## Self-Check: PASSED

All files exist. Both task commits verified in git history (4740f64, 09f771d).

---
*Phase: 07-credits-personnel*
*Completed: 2026-03-22*
