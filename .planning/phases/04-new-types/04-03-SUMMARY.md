---
phase: 04-new-types
plan: 03
subsystem: providers
tags: [kotlin, deezer, lastfm, fanarttv, enrichment-types, discography, tracklist, similar-tracks, banner]

requires:
  - phase: 04-new-types
    plan: 01
    provides: "6 new EnrichmentType enum values and EnrichmentData sealed subclasses (Discography, Tracklist, SimilarTracks)"
provides:
  - "Deezer ARTIST_DISCOGRAPHY capability via searchArtist + getArtistAlbums endpoints"
  - "Deezer ALBUM_TRACKS capability via searchAlbums + getAlbumTracks endpoints"
  - "Last.fm SIMILAR_TRACKS capability via track.getSimilar endpoint"
  - "Fanart.tv ARTIST_BANNER capability using existing banners data"
  - "DeezerMapper.toDiscography and toTracklist mapper methods"
  - "LastFmMapper.toSimilarTracks mapper method"
affects: [04-04]

tech-stack:
  added: []
  patterns: ["search-then-fetch pattern for Deezer (search for ID, then fetch by ID)"]

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerProvider.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProvider.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/deezer/DeezerProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProviderTest.kt

key-decisions:
  - "Deezer uses search-then-fetch pattern: searchArtist for ID, then getArtistAlbums/getAlbumTracks by ID"
  - "Last.fm SIMILAR_TRACKS accepts ForTrack requests while existing capabilities remain ForArtist-only"

patterns-established:
  - "Provider routing via when(type) in enrich() for multi-capability providers"
  - "buildTrackUrl helper for Last.fm methods requiring both track and artist parameters"

requirements-completed: [TYPE-02, TYPE-03, TYPE-04, TYPE-05]

duration: 3min
completed: 2026-03-21
---

# Phase 04 Plan 03: Provider New Capabilities Summary

**Deezer discography/tracks, Last.fm similar tracks, and Fanart.tv banner capabilities with search-then-fetch pattern and 7 new unit tests**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T09:11:57Z
- **Completed:** 2026-03-21T09:15:51Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- Deezer provider expanded from 1 to 3 capabilities (ALBUM_ART, ARTIST_DISCOGRAPHY, ALBUM_TRACKS) with searchArtist, getArtistAlbums, getAlbumTracks API methods
- Last.fm provider expanded from 4 to 5 capabilities with SIMILAR_TRACKS via track.getSimilar, accepting ForTrack requests
- Fanart.tv provider expanded from 5 to 6 capabilities with ARTIST_BANNER using existing parsed banner data
- Added DeezerArtistSearchResult, DeezerArtistAlbum, DeezerTrack models and LastFmSimilarTrack model
- Added DeezerMapper.toDiscography/toTracklist and LastFmMapper.toSimilarTracks mapper methods
- 7 new unit tests covering success and not-found cases for all new capabilities

## Task Commits

Each task was committed atomically:

1. **Task 1: Deezer discography/tracks API + Last.fm similar tracks API** - `3047f2a` (feat)
2. **Task 2: Provider capability declarations, routing, Fanart.tv banner, and tests** - `c4f24a7` (feat)

## Files Created/Modified
- `DeezerApi.kt` - Added searchArtist, getArtistAlbums, getAlbumTracks endpoints
- `DeezerModels.kt` - Added DeezerArtistSearchResult, DeezerArtistAlbum, DeezerTrack model classes
- `DeezerMapper.kt` - Added toDiscography and toTracklist mapper methods
- `DeezerProvider.kt` - Added ARTIST_DISCOGRAPHY and ALBUM_TRACKS capabilities with routing
- `LastFmApi.kt` - Added getSimilarTracks endpoint with buildTrackUrl helper
- `LastFmModels.kt` - Added LastFmSimilarTrack model class
- `LastFmMapper.kt` - Added toSimilarTracks mapper method
- `LastFmProvider.kt` - Added SIMILAR_TRACKS capability with ForTrack request handling
- `FanartTvProvider.kt` - Added ARTIST_BANNER capability using existing banners list
- `DeezerProviderTest.kt` - 3 new tests for discography, tracklist, and not-found cases
- `LastFmProviderTest.kt` - 2 new tests for similar tracks success and not-found
- `FanartTvProviderTest.kt` - 2 new tests for banner success and not-found

## Decisions Made
- Deezer uses search-then-fetch pattern: search for artist/album ID first, then use ID-based endpoints for data. Deezer IDs stored in `identifiers.extra["deezerId"]` for future lookups.
- Last.fm SIMILAR_TRACKS type check is handled before the ForArtist cast, allowing ForTrack requests for this type while all other types remain ForArtist-only.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 3 providers (Deezer, Last.fm, Fanart.tv) ready with new capabilities for plan 04-04
- Deezer: 3 capabilities (ALBUM_ART, ARTIST_DISCOGRAPHY, ALBUM_TRACKS)
- Last.fm: 5 capabilities (SIMILAR_ARTISTS, GENRE, ARTIST_BIO, ARTIST_POPULARITY, SIMILAR_TRACKS)
- Fanart.tv: 6 capabilities (ARTIST_PHOTO, ARTIST_BACKGROUND, ARTIST_LOGO, ALBUM_ART, CD_ART, ARTIST_BANNER)
- All existing and new tests pass

---
*Phase: 04-new-types*
*Completed: 2026-03-21*
