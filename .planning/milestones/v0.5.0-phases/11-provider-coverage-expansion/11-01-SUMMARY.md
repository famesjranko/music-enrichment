---
phase: 11-provider-coverage-expansion
plan: 01
subsystem: api
tags: [lastfm, discogs, album-metadata, genre-tags, community-rating, provider-chain]

# Dependency graph
requires:
  - phase: 10-genre-enhancement
    provides: GenreTag type with confidence scores and source list
  - phase: 07-credits-personnel
    provides: DiscogsReleaseDetail with extraartists and tracklist
provides:
  - Last.fm ALBUM_METADATA capability at priority 40 via album.getinfo
  - Discogs community rating (ratingCount, haveCount, wantCount) from release details
  - LastFmAlbumInfo model with tags, trackCount, wiki, playcount, listeners
  - DiscogsMapper.toAlbumMetadataFromDetail() for community data mapping
affects:
  - 11-02
  - 11-03

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ForAlbum type-guard dispatch before ForArtist cast (consistent with existing SIMILAR_TRACKS/TRACK_POPULARITY pattern)
    - Optional detail fetch after search: enrichAlbumMetadataWithCommunity() fetches release details if releaseId available

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmProviderTest.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapperTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsProviderTest.kt

key-decisions:
  - "Last.fm ALBUM_METADATA guard placed before ForArtist cast, consistent with SIMILAR_TRACKS/TRACK_POPULARITY pattern"
  - "enrichAlbumMetadata returns NotFound when album.getinfo parses but has no tags and no trackCount, avoiding empty Success results"
  - "enrichAlbumMetadataWithCommunity fetches release details only when releaseId is available from search result or identifiers — no extra API calls when ID is absent"
  - "FakeHttpClient URL key specificity: use 'database/search' instead of 'discogs.com' when also registering a 'releases/{id}' response to avoid match order collision"
  - "Parallel agent (11-03) committed DiscogsReleaseDetail community fields and toAlbumMetadataFromDetail as Rule 3 blocking fix; this plan's commit completed API extraction and provider wiring"

requirements-completed: [PROV-01, PROV-05]

# Metrics
duration: 18min
completed: 2026-03-22
---

# Phase 11 Plan 01: Last.fm Album Metadata + Discogs Community Rating Summary

**Last.fm ALBUM_METADATA at priority 40 via album.getinfo with genre tags, and Discogs community rating (average 4.2f, have/want counts) extracted from existing release details call**

## Performance

- **Duration:** ~18 min
- **Started:** 2026-03-22T05:00:00Z
- **Completed:** 2026-03-22T05:18:49Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments

- Last.fm now supplies ALBUM_METADATA at priority 40 with album-level genre tags (confidence 0.3f, source "lastfm"), trackCount from tracklist length, and wiki summary available via LastFmAlbumInfo
- Discogs release details now carry community rating, ratingCount, haveCount, wantCount; ALBUM_METADATA enrichment path fetches release details when releaseId available and merges communityRating into Metadata
- 10 new tests added (5 LastFm + 5 Discogs) covering success paths, not-found cases, null community data, and genreTags confidence verification

## Task Commits

Each task was committed atomically:

1. **Task 1: Last.fm album.getinfo API, model, mapper, and provider capability** - `55ea41b` (feat)
2. **Task 2: Discogs community rating and collector data from release details** - `99299ee` (feat)

**Plan metadata:** (this commit)

_Note: TDD tasks may have multiple commits (test -> feat -> refactor)_

## Files Created/Modified

- `LastFmModels.kt` - Added LastFmAlbumInfo data class (name, artist, playcount, listeners, tags, wiki, trackCount)
- `LastFmApi.kt` - Added getAlbumInfo(), buildAlbumUrl() helper, parseAlbumInfo() parser
- `LastFmMapper.kt` - Added toAlbumMetadata() mapping tags to GenreTag at confidence 0.3f
- `LastFmProvider.kt` - Added ALBUM_METADATA capability at priority 40, ForAlbum guard, enrichAlbumMetadata()
- `LastFmProviderTest.kt` - 5 new tests: capabilities check, success with trackCount/genres, genreTags, not-found, wrong-request-type
- `DiscogsModels.kt` - Extended DiscogsReleaseDetail with communityRating, ratingCount, haveCount, wantCount (null defaults)
- `DiscogsApi.kt` - Updated parseReleaseDetail() to extract community object with rating.average, rating.count, have, want
- `DiscogsMapper.kt` - Added toAlbumMetadataFromDetail() returning Metadata with communityRating
- `DiscogsProvider.kt` - Added enrichAlbumMetadataWithCommunity() fetching release details and merging communityRating
- `DiscogsMapperTest.kt` - 2 new tests: community rating mapping, null community data
- `DiscogsProviderTest.kt` - 3 new tests: community rating integration, null when no releaseId, parseReleaseDetail with community

## Decisions Made

- Last.fm ALBUM_METADATA guard placed before ForArtist cast, matching the existing SIMILAR_TRACKS/TRACK_POPULARITY dispatch pattern in LastFmProvider
- enrichAlbumMetadata returns NotFound when album info parses but contains no tags and no trackCount — avoids empty Success results for error responses that parse as valid JSON (e.g., `{"error":6}` with no album object returns null from parseAlbumInfo)
- enrichAlbumMetadataWithCommunity fetches release details only when a releaseId is available from the search result or passed identifiers — no extra API calls when ID is absent, preserving the "zero extra calls" promise of the plan
- FakeHttpClient URL key collision: `"discogs.com"` matches both `database/search` and `releases/{id}` URLs (both contain `api.discogs.com`). Fixed by using `"database/search"` as the key for search responses when also registering a `"releases/99001"` response

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing `exactMatch()` unresolved reference in ITunesProvider.kt**
- **Found during:** Task 2 (Discogs tests) — compilation failed with `Unresolved reference: exactMatch`
- **Issue:** `ConfidenceCalculator.exactMatch()` was called in ITunesProvider.kt line 84 but that method doesn't exist in ConfidenceCalculator
- **Fix:** Parallel agent (11-03) fixed this as Rule 1 during their execution by replacing with `idBasedLookup()` — was already committed by the time this plan's Task 2 ran
- **Files modified:** ITunesProvider.kt (committed in `69b5acd` by parallel agent)
- **Committed in:** `69b5acd` (parallel agent 11-03 commit)

**2. [Rule 3 - Blocking] Parallel agent pre-committed DiscogsReleaseDetail community fields**
- **Found during:** Task 2 preparation — parallel agent 11-03 committed DiscogsModels.kt + DiscogsMapper.kt community changes as a Rule 3 fix to unblock their own build
- **Issue:** The parallel 11-03 agent needed these types to compile its tests; both plans needed the same fields
- **Fix:** No duplicate work — agent 11-03's commit included the model/mapper stub; this plan completed the API extraction and provider wiring in DiscogsApi.kt and DiscogsProvider.kt
- **Files modified:** DiscogsModels.kt, DiscogsMapper.kt (committed in `69b5acd`)
- **Committed in:** `69b5acd` (parallel agent) + `99299ee` (this plan)

---

**Total deviations:** 2 noted (both from parallel execution coordination, not scope changes)
**Impact on plan:** All acceptance criteria met. Parallel agent handled blocking compilation errors. This plan completed all wiring and added tests.

## Issues Encountered

- FakeHttpClient URL key collision: both `database/search` and `releases/{id}` URLs contain `api.discogs.com`. Test initially used `"discogs.com"` as key for search responses, causing FakeHttpClient to return search JSON when the release detail endpoint was called (first match wins). Fixed by using `"database/search"` as a more specific key in the community rating provider test.

## Next Phase Readiness

- Plans 11-02 and 11-03 can proceed — Last.fm ALBUM_METADATA at priority 40 is live, Discogs community rating is available in all ALBUM_METADATA results where a releaseId was found during search
- No blockers

---
*Phase: 11-provider-coverage-expansion*
*Completed: 2026-03-22*
