---
phase: 04-new-types
plan: 02
subsystem: provider
tags: [kotlin, musicbrainz, band-members, discography, tracklist, artist-links, enrichment-types]

requires:
  - phase: 04-new-types
    plan: 01
    provides: "6 new EnrichmentType enum values, 5 new EnrichmentData sealed subclasses, supporting data classes"
provides:
  - "MusicBrainz provider with 4 new capabilities: BAND_MEMBERS, ARTIST_DISCOGRAPHY, ALBUM_TRACKS, ARTIST_LINKS"
  - "browseReleaseGroups() and lookupArtistWithRels() API endpoints"
  - "parseBandMembers, parseReleaseGroups, parseMedia, parseUrlRelations parser methods"
  - "toBandMembers, toDiscography, toTracklist, toArtistLinks mapper methods"
  - "4 new MusicBrainz DTO models: MusicBrainzBandMember, MusicBrainzReleaseGroup, MusicBrainzTrack, MusicBrainzUrlRelation"
affects: [04-03, 04-04]

tech-stack:
  added: []
  patterns: ["artist-rels inc param for band member relationships", "enrichArtistNewType routing pattern for new type dispatch"]

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParser.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParserTest.kt

key-decisions:
  - "Separate lookupArtistWithRels method (artist-rels inc param) to avoid overhead on existing artist lookups"
  - "ARTIST_NEW_TYPES set routes new types through enrichArtistNewType for clean dispatch"
  - "urlRelations and bandMembers fields added to MusicBrainzArtist model, populated during parsing"
  - "tracks field added to MusicBrainzRelease model, media+recordings added to release lookup inc params"

patterns-established:
  - "New enrichment types dispatch through enrichArtistNewType with MBID resolution then type-specific API call"
  - "Model DTOs carry parsed sub-structures (urlRelations, bandMembers, tracks) populated during JSON parsing"

requirements-completed: [TYPE-01, TYPE-02, TYPE-03, TYPE-06]

duration: 7min
completed: 2026-03-21
---

# Phase 04 Plan 02: MusicBrainz New Types Summary

**MusicBrainz provider expanded from 5 to 9 capabilities with band members from artist-rels, discography from browse endpoint, tracklist from media array, and artist links from url-rels**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-21T09:11:41Z
- **Completed:** 2026-03-21T09:18:50Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Added browseReleaseGroups() and lookupArtistWithRels() API endpoints to MusicBrainzApi
- Added 4 parse methods (parseBandMembers, parseReleaseGroups, parseMedia, parseUrlRelations) and 4 mapper methods (toBandMembers, toDiscography, toTracklist, toArtistLinks)
- Extended MusicBrainzProvider from 5 to 9 capabilities with routing for all 4 new enrichment types
- Added 4 new MusicBrainz DTO models and extended existing MusicBrainzArtist and MusicBrainzRelease with new fields
- Parser tests verify JSON parsing for band members, release groups, media tracks, and URL relations
- Provider tests cover success and not-found paths for BAND_MEMBERS, ARTIST_DISCOGRAPHY, ALBUM_TRACKS, ARTIST_LINKS

## Task Commits

Each task was committed atomically:

1. **Task 1: Add MusicBrainz API endpoints, parser methods, and models** - `b664315` (feat)
2. **Task 2: Add MusicBrainz provider capabilities, routing, and tests** - `8591bc0` (feat)

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzModels.kt` - 4 new DTO data classes, extended MusicBrainzArtist and MusicBrainzRelease
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzApi.kt` - browseReleaseGroups() and lookupArtistWithRels() endpoints, media+recordings in release lookup
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParser.kt` - 4 new parse methods, auto-populate urlRelations/bandMembers/tracks during parsing
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzMapper.kt` - toBandMembers, toDiscography, toTracklist, toArtistLinks mapper methods
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt` - 4 new capabilities, enrichArtistNewType routing, enrichAlbumTracks
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParserTest.kt` - 4 new parser tests with JSON fixtures
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProviderTest.kt` - 5 new provider tests (4 success + 1 not-found)

## Decisions Made
- Separate lookupArtistWithRels method avoids adding artist-rels overhead to existing artist lookups that only need url-rels
- New artist types routed through enrichArtistNewType for clean separation from existing metadata enrichment
- URL relations and band members populated during JSON parsing (in parseArtistObject) so no separate API call needed when data is already in response
- media+recordings added to release lookup inc params so tracks are available on all release lookups

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- MusicBrainz provider now supports all 4 new types with full test coverage
- Ready for plans 04-03 and 04-04 to add these types to other providers

## Self-Check: PASSED

All 7 files verified present, both commits (b664315, 8591bc0) verified in git log.

---
*Phase: 04-new-types*
*Completed: 2026-03-21*
