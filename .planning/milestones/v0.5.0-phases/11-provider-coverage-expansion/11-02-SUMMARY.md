---
phase: 11-provider-coverage-expansion
plan: "02"
subsystem: provider/itunes
tags: [itunes, album-tracks, artist-discography, lookup-api, tdd]
dependency_graph:
  requires: []
  provides: [PROV-02]
  affects: [ITunesProvider, ITunesApi, ITunesMapper, ITunesModels]
tech_stack:
  added: []
  patterns: [TDD Red-Green, id-first lookup with search fallback, resolvedIdentifiers for ID storage]
key_files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProviderTest.kt
decisions:
  - ITunesProvider uses id-first lookup (collectionId/artistId) with search fallback for ALBUM_TRACKS; ARTIST_DISCOGRAPHY requires stored itunesArtistId or searchArtist() because ForArtist has no album context
  - ITunesAlbumResult reused for discography lookup results (wrapperType==collection filter) — no separate artist album model needed
  - itunesCollectionId stored via resolvedIdentifiers.extra on ALBUM_TRACKS success so downstream enrichment can use direct lookup
  - searchArtist() added as new API method for artist name to artistId resolution (ForArtist discography without stored ID)
metrics:
  duration_seconds: 398
  completed_date: "2026-03-22"
  tasks_completed: 1
  files_modified: 5
---

# Phase 11 Plan 02: iTunes ALBUM_TRACKS and ARTIST_DISCOGRAPHY Summary

iTunes lookup API integration with collectionId/artistId storage for ALBUM_TRACKS and ARTIST_DISCOGRAPHY capabilities at priority 30.

## Completed Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | iTunes lookup API methods, models, and mapper functions (TDD) | 8098f00 (RED), 882c353 (GREEN) | ITunesApi, ITunesModels, ITunesMapper, ITunesProvider, ITunesProviderTest |

## What Was Built

### ITunesModels.kt
Added `ITunesTrackResult` data class for iTunes Lookup API track results, capturing `trackId`, `trackName`, `trackNumber`, `trackTimeMillis`, `artistName`, and `collectionName`.

### ITunesApi.kt
Three new API methods:
- `lookupAlbumTracks(collectionId: Long)` — fetches `/lookup?id={id}&entity=song`, filters `wrapperType==track`
- `lookupArtistAlbums(artistId: Long)` — fetches `/lookup?id={id}&entity=album`, filters `wrapperType==collection`
- `searchArtist(artistName: String)` — fetches `/search?entity=musicArtist&limit=1`, returns artistId

Refactored `parseAlbumResult` into a shared private helper (DRY — used by both `searchAlbums` and `lookupArtistAlbums`).

### ITunesMapper.kt
Two new mapper functions:
- `toTracklist(tracks: List<ITunesTrackResult>)` — maps to `EnrichmentData.Tracklist` with track titles, positions, durations
- `toDiscography(albums: List<ITunesAlbumResult>)` — maps to `EnrichmentData.Discography` with album titles, years, thumbnails, and per-album `itunesCollectionId` in identifiers

### ITunesProvider.kt
- Added `ALBUM_TRACKS` capability at priority 30
- Added `ARTIST_DISCOGRAPHY` capability at priority 30
- Routing: `enrich()` dispatches to `enrichAlbumTracks()` / `enrichArtistDiscography()` / `enrichAlbumType()`
- `enrichAlbumTracks()`: ForAlbum required; uses stored `itunesCollectionId` for direct lookup or falls back to search-then-lookup; stores `itunesCollectionId` on `resolvedIdentifiers`
- `enrichArtistDiscography()`: ForArtist required; uses stored `itunesArtistId` or `searchArtist()` fallback
- `buildResolvedIdentifiers()`: stores `itunesCollectionId` on `EnrichmentIdentifiers.extra`

### ITunesProviderTest.kt
8 new tests added (19 total, all passing):
- `capabilities include ALBUM_TRACKS and ARTIST_DISCOGRAPHY at priority 30`
- `enrich returns album tracks from lookup API`
- `enrich returns album tracks by search when no collectionId`
- `enrich returns artist discography from lookup API`
- `enrich returns artist discography by search when no artistId`
- `enrich returns NotFound for ALBUM_TRACKS when lookup returns empty`
- `enrich stores itunesCollectionId and itunesArtistId on resolvedIdentifiers`
- `enrich returns NotFound for ARTIST_DISCOGRAPHY with ForAlbum request`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Parallel agent test compilation blocked build**

- **Found during:** Task 1 GREEN phase
- **Issue:** Parallel agent 11-04 wrote `DiscogsMapperTest.kt` tests referencing `DiscogsMapper.toAlbumMetadataFromDetail()` which didn't exist yet, blocking compilation of all test sources.
- **Fix:** Added `toAlbumMetadataFromDetail(detail: DiscogsReleaseDetail)` to `DiscogsMapper` returning `EnrichmentData.Metadata(communityRating = detail.communityRating)`. Note: the parallel agent 11-03 (ListenBrainz) also independently added this method and related `DiscogsReleaseDetail` community fields, so these are now committed via that agent's work.
- **Files modified:** `DiscogsMapper.kt` (linter applied the fix before explicit edit)

**2. [Rule 1 - Bug] exactMatch() is not a method on ConfidenceCalculator**

- **Found during:** Task 1 GREEN phase
- **Issue:** Initial implementation used `ConfidenceCalculator.exactMatch()` which does not exist. Available methods are `idBasedLookup()`, `authoritative()`, `searchScore()`, `fuzzyMatch()`.
- **Fix:** Changed to `ConfidenceCalculator.idBasedLookup()` for direct ID-based lookup path.
- **Files modified:** `ITunesProvider.kt` (committed by parallel agent 11-03 alongside this fix)

### Out-of-scope Issues (deferred)

- `ListenBrainzProviderTest` has 3 failing tests for SIMILAR_ARTISTS — these are from parallel agent 11-03 working on ListenBrainz. Out of scope for this plan.

## Verification Results

```
19 tests completed, 0 failures, 0 errors (ITunesProviderTest)
All acceptance criteria passed:
  PASS: lookupAlbumTracks
  PASS: lookupArtistAlbums
  PASS: ITunesTrackResult
  PASS: toTracklist
  PASS: toDiscography
  PASS: ALBUM_TRACKS
  PASS: ARTIST_DISCOGRAPHY
  PASS: itunesCollectionId
  PASS: album tracks test
  PASS: discography test
```

## Known Stubs

None — all data is wired from real API responses.

## Self-Check: PASSED
