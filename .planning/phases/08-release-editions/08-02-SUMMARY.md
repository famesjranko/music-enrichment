---
phase: 08-release-editions
plan: "02"
subsystem: provider
tags: [discogs, release-editions, kotlin, tdd]

# Dependency graph
requires:
  - phase: 08-01
    provides: ReleaseEditions/ReleaseEdition data classes and RELEASE_EDITIONS EnrichmentType
  - phase: 06-04
    provides: discogsMasterId stored in identifiers.extra by Discogs identity resolution
provides:
  - DiscogsMasterVersion data class for master versions API response
  - DiscogsApi.getMasterVersions fetching /masters/{id}/versions
  - DiscogsMapper.toReleaseEditions converting versions to ReleaseEditions
  - DiscogsProvider RELEASE_EDITIONS capability at priority 50 (fallback behind MusicBrainz)
  - enrichAlbumEditions reading discogsMasterId from identifiers.extra
affects: [provider-chain, release-editions, discogs]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - RELEASE_EDITIONS dispatch guard in enrich() after CREDITS guard, same pattern as prior type guards
    - getMasterVersions follows same nullable-return pattern as getReleaseDetails

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapperTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt

key-decisions:
  - "RELEASE_EDITIONS dispatch guard placed after CREDITS guard in enrich(), consistent with established type-check guard pattern"
  - "enrichAlbumEditions reads discogsMasterId from identifiers.extra (Phase 6 DEBT-04 dependency), not by fuzzy search"
  - "Confidence set to fuzzyMatch(hasArtistMatch=false)=0.6f — consistent with other Discogs enrichment paths"
  - "barcode=null in toReleaseEditions — Discogs master versions API does not include barcode"

patterns-established:
  - "RELEASE_EDITIONS dispatch: check type, cast to ForAlbum or NotFound, try/catch with mapError"
  - "DiscogsMasterVersion id > 0 convention for absent ID (same as masterId=0 convention in Phase 6)"

requirements-completed: [EDIT-03]

# Metrics
duration: 8min
completed: 2026-03-21
---

# Phase 08 Plan 02: Discogs RELEASE_EDITIONS Fallback Summary

**Discogs master versions endpoint wired as RELEASE_EDITIONS fallback (priority 50) reading discogsMasterId from identifiers.extra, with DiscogsMasterVersion model, getMasterVersions API, and toReleaseEditions mapper**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-21T17:00:00Z
- **Completed:** 2026-03-21T17:08:39Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Added DiscogsMasterVersion data class (id, title, format, label, country, year, catno)
- Added getMasterVersions API method fetching /masters/{id}/versions?per_page=100 with token auth
- Added toReleaseEditions mapper converting versions list to EnrichmentData.ReleaseEditions with discogsReleaseId in identifiers.extra
- Wired RELEASE_EDITIONS capability at priority 50 in DiscogsProvider with enrichAlbumEditions reading discogsMasterId from identifiers.extra
- 10 new tests: 4 mapper tests, 6 provider tests

## Task Commits

Each task was committed atomically:

1. **Task 1: Discogs master versions API + model + mapper** - `39448c2` (feat)
2. **Task 2: Wire Discogs RELEASE_EDITIONS capability into Provider** - `6bc38f8` (feat)

_Note: TDD tasks committed after GREEN phase (tests written first, then implementation)_

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt` - Added DiscogsMasterVersion data class
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt` - Added MASTERS_URL, getMasterVersions, parseMasterVersions
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt` - Added toReleaseEditions
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt` - Added RELEASE_EDITIONS capability, dispatch guard, enrichAlbumEditions
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapperTest.kt` - Added 4 toReleaseEditions tests
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt` - Added 6 RELEASE_EDITIONS provider tests

## Decisions Made
- RELEASE_EDITIONS dispatch guard placed after CREDITS guard in enrich(), consistent with established type-check guard pattern
- enrichAlbumEditions reads discogsMasterId from identifiers.extra (stored by Phase 6 DEBT-04), not by fuzzy search — enables precise lookup
- Confidence set to fuzzyMatch(hasArtistMatch=false)=0.6f — consistent with other Discogs enrichment paths that don't artist-match
- barcode=null in toReleaseEditions — Discogs master versions API flat response does not include barcode field

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- RELEASE_EDITIONS now has both MusicBrainz (priority 100) and Discogs (priority 50) providers
- Phase 08 complete: both plans delivered the full RELEASE_EDITIONS capability
- Ready for Phase 09 (ARTIST_TIMELINE) or next planned phase

## Self-Check: PASSED

All created/modified files verified present. All task commits verified in git log.

---
*Phase: 08-release-editions*
*Completed: 2026-03-21*
