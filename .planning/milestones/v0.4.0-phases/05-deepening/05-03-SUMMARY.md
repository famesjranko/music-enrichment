---
phase: 05-deepening
plan: 03
subsystem: providers
tags: [deezer, itunes, discogs, wikipedia, album-metadata, artist-photo, media-list]

requires:
  - phase: 04-new-types
    provides: "Provider mapper pattern and enrichment type infrastructure"
  - phase: 05-deepening
    provides: "Deepening patterns from plans 01-02"
provides:
  - "ALBUM_METADATA enrichment type with 90-day TTL"
  - "Metadata fields: trackCount, explicit, catalogNumber, communityRating"
  - "Deezer ALBUM_METADATA (priority 50) with nb_tracks, record_type, explicit_lyrics"
  - "iTunes ALBUM_METADATA (priority 30) with trackCount, genre, country"
  - "Discogs ALBUM_METADATA (priority 40) with catno, genre, style arrays"
  - "Wikipedia ARTIST_PHOTO (priority 30) via page media-list endpoint"
affects: [confidence-standardization, provider-documentation]

tech-stack:
  added: []
  patterns: ["media-list filtering (SVG/icon/tiny-image exclusion)", "album metadata mining from existing search responses"]

key-files:
  created: []
  modified:
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerModels.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerApi.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesModels.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesApi.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaModels.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaApi.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaMapper.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaProvider.kt"

key-decisions:
  - "ALBUM_METADATA as new EnrichmentType rather than enriching existing GENRE/LABEL types, matching roadmap success criteria"
  - "Wikipedia ARTIST_PHOTO at priority 30 and confidence 0.7 to rank behind Wikidata (100/0.9) and Fanart.tv (80)"
  - "Media-list filtering excludes SVGs, icons, logos, and images smaller than 100px width"

patterns-established:
  - "Album metadata mining: parse additional fields from existing search API responses without new endpoints"
  - "Media-list filtering: title-based (.svg, icon, logo) plus dimension-based (<100px) exclusion for Wikipedia images"

requirements-completed: [DEEP-02, DEEP-04]

duration: 5min
completed: 2026-03-21
---

# Phase 05 Plan 03: Album Metadata and Wikipedia Artist Photo Summary

**ALBUM_METADATA type served by Deezer (priority 50), iTunes (30), Discogs (40) mining previously ignored search fields; Wikipedia ARTIST_PHOTO via page media-list as supplemental source (priority 30)**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T09:54:57Z
- **Completed:** 2026-03-21T10:00:00Z
- **Tasks:** 2
- **Files modified:** 18

## Accomplishments
- Added ALBUM_METADATA enrichment type extracting trackCount, explicit flag, catalog number, and genres from Deezer, iTunes, and Discogs album search responses
- Extended Metadata data class with trackCount, explicit, catalogNumber, communityRating fields
- WikipediaProvider serves ARTIST_PHOTO via page media-list endpoint with SVG/icon/tiny-image filtering

## Task Commits

Each task was committed atomically:

1. **Task 1: Album metadata from Deezer, iTunes, Discogs** - `4c87b0d` (test) + `da34c11` (feat)
2. **Task 2: Wikipedia page media-list for ARTIST_PHOTO** - `854b1f3` (test) + `7ad523c` (feat)

_TDD: Each task has separate RED (test) and GREEN (feat) commits._

## Files Created/Modified
- `EnrichmentType.kt` - Added ALBUM_METADATA enum entry with 90-day TTL
- `EnrichmentData.kt` - Added trackCount, explicit, catalogNumber, communityRating to Metadata
- `DeezerModels.kt` - Added nbTracks, recordType, explicitLyrics fields to DeezerAlbumResult
- `DeezerApi.kt` - Parse nb_tracks, record_type, explicit_lyrics from album search JSON
- `DeezerMapper.kt` - Added toAlbumMetadata() mapping DeezerAlbumResult to Metadata
- `DeezerProvider.kt` - Added ALBUM_METADATA capability (priority 50) and enrichAlbumMetadata()
- `ITunesModels.kt` - Added trackCount field to ITunesAlbumResult
- `ITunesApi.kt` - Parse trackCount from album search JSON
- `ITunesMapper.kt` - Added toAlbumMetadata() with trackCount, genres, country, releaseDate
- `ITunesProvider.kt` - Added ALBUM_METADATA capability (priority 30), refactored enrich() dispatch
- `DiscogsModels.kt` - Added catno, genres, styles fields to DiscogsRelease
- `DiscogsApi.kt` - Parse catno, genre array, style array from release search JSON
- `DiscogsMapper.kt` - Added toAlbumMetadata() combining genres+styles, catalogNumber, label
- `DiscogsProvider.kt` - Added ALBUM_METADATA capability (priority 40) in enrichFromRelease()
- `WikipediaModels.kt` - Added WikipediaMediaItem data class
- `WikipediaApi.kt` - Added getPageMediaList() with SVG/icon/size filtering
- `WikipediaMapper.kt` - Added toArtwork() mapping WikipediaMediaItem to Artwork
- `WikipediaProvider.kt` - Added ARTIST_PHOTO capability (priority 30), refactored to enrichBio()/enrichArtistPhoto()

## Decisions Made
- ALBUM_METADATA as a distinct EnrichmentType rather than enriching existing GENRE/LABEL types, matching the roadmap success criteria format
- Wikipedia ARTIST_PHOTO confidence set to 0.7 (lower than Wikidata 0.9) since Wikipedia images are less curated
- Media-list filtering strategy: title-based (.svg extension, "icon"/"logo" substring) plus dimension-based (<100px width) to exclude decorative images

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- ALBUM_METADATA chain ready: Deezer (50) > Discogs (40) > iTunes (30)
- Wikipedia ARTIST_PHOTO supplements Wikidata and Fanart.tv in the photo chain
- Ready for plan 04 (confidence standardization)

## Self-Check: PASSED

All 8 key files verified present. All 4 task commits verified in git log. All 8 acceptance criteria pass.

---
*Phase: 05-deepening*
*Completed: 2026-03-21*
