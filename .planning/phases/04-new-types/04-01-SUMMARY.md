---
phase: 04-new-types
plan: 01
subsystem: api
tags: [kotlin, kotlinx-serialization, enrichment-types, data-models]

requires:
  - phase: 03-public-api-cleanup
    provides: "TTL in EnrichmentType enum, @Serializable EnrichmentIdentifiers with extra map"
provides:
  - "6 new EnrichmentType enum values (BAND_MEMBERS, ARTIST_DISCOGRAPHY, ALBUM_TRACKS, SIMILAR_TRACKS, ARTIST_BANNER, ARTIST_LINKS)"
  - "5 new EnrichmentData sealed subclasses (BandMembers, Discography, Tracklist, SimilarTracks, ArtistLinks)"
  - "6 supporting data classes (BandMember, DiscographyAlbum, TrackInfo, SimilarTrack, ExternalLink, ArtworkSize)"
  - "Enhanced Artwork with sizes field"
affects: [04-02, 04-03, 04-04]

tech-stack:
  added: []
  patterns: ["top-level @Serializable data classes for list elements referenced by sealed subclasses"]

key-files:
  created:
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/EnrichmentDataSerializationTest.kt
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt

key-decisions:
  - "Supporting data classes placed as top-level @Serializable types (consistent with existing SimilarArtist, PopularTrack pattern)"

patterns-established:
  - "New EnrichmentData subclasses wrap List<T> of a top-level @Serializable data class that carries EnrichmentIdentifiers"

requirements-completed: [TYPE-01, TYPE-02, TYPE-03, TYPE-04, TYPE-05, TYPE-06, TYPE-07]

duration: 2min
completed: 2026-03-21
---

# Phase 04 Plan 01: New Type Definitions Summary

**6 new EnrichmentType enum values, 5 sealed subclasses, 6 supporting data classes, ArtworkSize with sizes field on Artwork, all with serialization round-trip tests**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-21T09:06:39Z
- **Completed:** 2026-03-21T09:09:13Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Added 6 new EnrichmentType enum values with appropriate TTL values (BAND_MEMBERS, SIMILAR_TRACKS, ARTIST_LINKS at 30/90 days; ARTIST_DISCOGRAPHY at 30 days; ALBUM_TRACKS at 365 days; ARTIST_BANNER at 90 days)
- Added 5 new EnrichmentData sealed subclasses: BandMembers, Discography, Tracklist, SimilarTracks, ArtistLinks
- Added 6 supporting top-level data classes: BandMember, DiscographyAlbum, TrackInfo, SimilarTrack, ExternalLink, ArtworkSize -- all @Serializable with EnrichmentIdentifiers where applicable
- Enhanced Artwork with optional sizes: List<ArtworkSize> field for multiple image sizes
- Created comprehensive serialization round-trip tests for all new types

## Task Commits

Each task was committed atomically:

1. **Task 1: Add new EnrichmentType values and ArtworkSize + enhance Artwork** - `8dbfcf9` (feat)
2. **Task 2: Add new EnrichmentData subclasses with serialization tests** - `0595b7b` (feat)

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt` - 6 new enum values with TTL
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt` - 5 sealed subclasses, 6 supporting data classes, ArtworkSize, enhanced Artwork
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/EnrichmentDataSerializationTest.kt` - Round-trip serialization tests for all new types
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt` - Updated exhaustive when expression for new subclasses

## Decisions Made
- Supporting data classes placed as top-level @Serializable types, consistent with existing SimilarArtist and PopularTrack pattern

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed exhaustive when expression in EnrichmentShowcaseTest**
- **Found during:** Task 2 (compilation of test sources)
- **Issue:** Existing `snippet()` function in EnrichmentShowcaseTest.kt had an exhaustive `when` on EnrichmentData sealed class, missing branches for the 5 new subclasses
- **Fix:** Added display branches for BandMembers, Discography, Tracklist, SimilarTracks, ArtistLinks
- **Files modified:** musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt
- **Verification:** compileTestKotlin and full test suite pass
- **Committed in:** 0595b7b (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Necessary fix for compilation. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All type contracts established for plans 04-02, 04-03, and 04-04 to implement providers against
- EnrichmentType has 22 values total (16 existing + 6 new)
- All new data classes compile and serialize correctly

## Self-Check: PASSED

All files verified present, all commits verified in git log.

---
*Phase: 04-new-types*
*Completed: 2026-03-21*
