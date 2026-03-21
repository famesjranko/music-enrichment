---
phase: 11-provider-coverage-expansion
plan: "03"
subsystem: providers
tags: [fanart-tv, listenbrainz, album-art, similar-artists, tdd]
dependency_graph:
  requires: []
  provides: [fanart-tv-album-endpoint, listenbrainz-similar-artists]
  affects: [FanartTvProvider, FanartTvApi, FanartTvModels, ListenBrainzProvider, ListenBrainzApi, ListenBrainzModels, ListenBrainzMapper]
tech_stack:
  added: []
  patterns: [album-first-with-artist-fallback, tdd-red-green]
key_files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProviderTest.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/listenbrainz/ListenBrainzProviderTest.kt
decisions:
  - "Fanart.tv album-first strategy: try album-specific endpoint before artist endpoint; null return signals fall-through"
  - "enrichAlbumArtFromAlbumEndpoint returns EnrichmentResult? where null means fall through to artist endpoint, not a real NotFound"
  - "ListenBrainz getSimilarArtists uses fetchJsonResult (not fetchJsonArrayResult) because /explore/lb-radio endpoint returns JSONObject with payload array"
metrics:
  duration: 396s
  completed_date: "2026-03-21T18:19:43Z"
  tasks_completed: 2
  files_modified: 9
requirements_satisfied: [PROV-03, PROV-04]
---

# Phase 11 Plan 03: Provider Coverage Expansion (Fanart.tv + ListenBrainz) Summary

Fanart.tv album-specific endpoint with artist fallback for ALBUM_ART/CD_ART, and ListenBrainz SIMILAR_ARTISTS at priority 50 using the lb-radio similar-artists API.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Fanart.tv album-specific art endpoint (PROV-03) | 0db9d6d | FanartTvApi, FanartTvModels, FanartTvProvider, FanartTvProviderTest |
| 2 | ListenBrainz similar artists API/model/mapper/provider (PROV-04) | 3b2707c | ListenBrainzApi, ListenBrainzModels, ListenBrainzMapper, ListenBrainzProvider, ListenBrainzProviderTest |

## What Was Built

### Task 1: Fanart.tv Album-First Strategy (PROV-03)

Added `FanartTvAlbumImages` data class holding `albumCovers` and `cdArt` lists. Added `getAlbumImages(releaseGroupMbid)` to `FanartTvApi` which hits the Fanart.tv album endpoint (`/v3/music/albums/{releaseGroupMbid}`). The response is a nested JSON object `{ "{mbid}": { "albumcover": [...], "cdart": [...] } }`.

Updated `FanartTvProvider.enrich()` to attempt the album endpoint first for ALBUM_ART and CD_ART when `musicBrainzReleaseGroupId` is available, falling back to the existing artist endpoint when the album endpoint returns null or empty images. The logic is extracted into `enrichAlbumArtFromAlbumEndpoint()` which returns `EnrichmentResult?` where null signals fall-through.

### Task 2: ListenBrainz Similar Artists (PROV-04)

Added `ListenBrainzSimilarArtist` data class with `artistMbid`, `name`, `score`. Added `getSimilarArtists(artistMbid, count=20)` to `ListenBrainzApi` which calls `/1/explore/lb-radio/artist/{mbid}/similar` — this endpoint returns a JSONObject (not array), parsed via `fetchJsonResult` then extracting `payload` array.

Added `toSimilarArtists()` to `ListenBrainzMapper` mapping to `EnrichmentData.SimilarArtists`. Added `SIMILAR_ARTISTS` capability at priority 50 with `MUSICBRAINZ_ID` requirement and `enrichSimilarArtists()` private method to `ListenBrainzProvider`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed ITunesProvider.exactMatch() unresolved reference**
- **Found during:** Task 2 RED phase (compilation error blocking test run)
- **Issue:** `ConfidenceCalculator.exactMatch()` called at line 84 of `ITunesProvider.kt` but the method doesn't exist — `ConfidenceCalculator` has `idBasedLookup()`, `authoritative()`, `fuzzyMatch()` only
- **Fix:** Changed to `ConfidenceCalculator.idBasedLookup()` (semantically correct — direct collectionId lookup is an id-based lookup)
- **Files modified:** `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProvider.kt`
- **Commit:** 69b5acd

**2. [Rule 3 - Blocking] Added DiscogsReleaseDetail community rating fields**
- **Found during:** Task 2 RED phase (compilation error blocking test run)
- **Issue:** `DiscogsMapperTest.kt` referenced `communityRating`, `ratingCount`, `haveCount`, `wantCount` on `DiscogsReleaseDetail` which was missing those fields — likely written by a parallel plan
- **Fix:** Added optional fields to `DiscogsReleaseDetail` data class
- **Files modified:** `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt`
- **Commit:** 69b5acd

**3. [Rule 3 - Blocking] Added DiscogsMapper.toAlbumMetadataFromDetail**
- **Found during:** Task 2 RED phase (compilation error blocking test run)
- **Issue:** `DiscogsMapperTest.kt` referenced `DiscogsMapper.toAlbumMetadataFromDetail()` which didn't exist — likely written by a parallel plan
- **Fix:** Added `toAlbumMetadataFromDetail(detail: DiscogsReleaseDetail): EnrichmentData.Metadata` mapping `communityRating`
- **Files modified:** `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt`
- **Commit:** 69b5acd

## Key Decisions

1. **Fanart.tv album-first null-return pattern**: `enrichAlbumArtFromAlbumEndpoint()` returns `EnrichmentResult?` where null means "no data from album endpoint, try artist endpoint". This avoids returning NotFound prematurely and allows clean fall-through.

2. **ListenBrainz uses fetchJsonResult not fetchJsonArrayResult**: The `/explore/lb-radio/artist/{mbid}/similar` endpoint returns `{"payload": [...]}` (a JSONObject wrapper), unlike the other ListenBrainz endpoints that return raw JSONArrays.

## Test Coverage

| Test | Description |
|------|-------------|
| enrich ALBUM_ART uses album endpoint when releaseGroupMbid available | Album endpoint hit when rg MBID set |
| enrich ALBUM_ART falls back to artist endpoint when album endpoint returns null | Artist fallback works |
| enrich ALBUM_ART uses artist endpoint when no releaseGroupMbid | Artist path for MBID-only requests |
| enrich CD_ART uses album endpoint when releaseGroupMbid available | CD_ART also uses album endpoint |
| enrich ALBUM_ART returns NotFound when both endpoints return nothing | Both empty = NotFound |
| enrich returns similar artists from ListenBrainz | Success with names, MBIDs, scores |
| enrich returns NotFound for SIMILAR_ARTISTS when API returns empty | Empty payload = NotFound |
| enrich returns NotFound for SIMILAR_ARTISTS without musicBrainzId | No MBID = NotFound |
| capabilities include SIMILAR_ARTISTS at priority 50 | Priority and requirement verified |
| enrich returns Error with NETWORK ErrorKind when similar artists API fails | IOException maps to NETWORK |

## Known Stubs

None.

## Self-Check: PASSED
