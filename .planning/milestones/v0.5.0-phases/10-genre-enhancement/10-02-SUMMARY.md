---
phase: 10-genre-enhancement
plan: "02"
subsystem: genre-enhancement
tags: [genre, genreTag, mapper, musicbrainz, lastfm, discogs, itunes]
dependency_graph:
  requires: [10-01]
  provides: [genreTags populated on all 4 provider mappers]
  affects: [EnrichmentData.Metadata]
tech_stack:
  added: []
  patterns: [mapper-genreTag-population, tagCounts-preservation]
key_files:
  created:
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzMapperGenreTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapperGenreTest.kt
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParser.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lastfm/LastFmMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/discogs/DiscogsMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesMapper.kt
decisions:
  - tagCounts preserved as List<Pair<String,Int>> on models rather than re-extracting in mapper — keeps mapper stateless and parser responsible for data extraction
  - extractReleaseTagCounts added alongside extractReleaseTags to maintain backward compatibility in parser API
  - extractTags refactored to delegate to extractTagsWithCounts — DRY, no behavior change for existing callers
  - Last.fm all tags get uniform 0.3f confidence (not position-decayed) — simple approach per plan spec
metrics:
  duration: 197s
  completed_date: "2026-03-21"
  tasks_completed: 2
  files_modified: 8
---

# Phase 10 Plan 02: Mapper Genre Tag Support Summary

All four genre-producing provider mappers updated to populate `genreTags: List<GenreTag>` with per-provider confidence scores while preserving the existing `genres: List<String>` field for backward compatibility per GENR-04.

## Tasks Completed

### Task 1: MusicBrainz and Last.fm mapper genre tag support (TDD)

**RED:** Created `MusicBrainzMapperGenreTest` and `LastFmMapperGenreTest` as failing tests.

**GREEN:**
- Added `tagCounts: List<Pair<String, Int>>` field to `MusicBrainzRelease`, `MusicBrainzArtist`, and `MusicBrainzRecording` in `MusicBrainzModels.kt`.
- Added `extractTagsWithCounts(obj: JSONObject): List<Pair<String, Int>>` to `MusicBrainzParser.kt`. Refactored `extractTags` to delegate to it. Added `extractReleaseTagCounts` (parallel to `extractReleaseTags`) for release-group fallback with counts.
- Updated `parseReleaseObject`, `parseArtistObject`, `parseRecordingObject` to populate `tagCounts` on models.
- Added `buildGenreTags` private helper to `MusicBrainzMapper` that converts tag count pairs to `GenreTag` with `confidence = 0.4f` and `sources = listOf("musicbrainz")`.
- Updated `toAlbumMetadata`, `toArtistMetadata`, `toTrackMetadata` in `MusicBrainzMapper` to set `genreTags`.
- Updated `LastFmMapper.toGenre` to also set `genreTags` with `confidence = 0.3f` and `sources = listOf("lastfm")`.

**Commit:** `4274abd`

### Task 2: Discogs and iTunes mapper genre tag support

- Updated `DiscogsMapper.toAlbumMetadata` to build `genreTags` list: `release.genres` at 0.3f confidence and `release.styles` at 0.2f confidence, both with `sources = listOf("discogs")`. Returns null if both lists are empty.
- Updated `ITunesMapper.toAlbumMetadata` to set `genreTags` from `primaryGenreName` at 0.2f confidence with `sources = listOf("itunes")`. Returns null if no genre.
- Both mappers retain existing `genres` field unchanged.

**Commit:** `6f235e5`

## Confidence Scheme

| Provider  | Source field  | Confidence | Notes                              |
|-----------|---------------|------------|------------------------------------|
| MusicBrainz | `musicbrainz` | 0.4f     | Uniform per tag (vote count preserved but not used in confidence formula) |
| Last.fm     | `lastfm`      | 0.3f     | Uniform per tag (position-sorted list) |
| Discogs genres | `discogs`  | 0.3f     | From release.genres field          |
| Discogs styles | `discogs`  | 0.2f     | From release.styles field          |
| iTunes      | `itunes`      | 0.2f     | From primaryGenreName              |

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

- MusicBrainzModels.kt tagCounts: FOUND
- MusicBrainzParser.kt extractTagsWithCounts: FOUND
- MusicBrainzMapper.kt genreTags: FOUND
- LastFmMapper.kt genreTags: FOUND
- DiscogsMapper.kt genreTags: FOUND
- ITunesMapper.kt genreTags: FOUND
- Commits 4274abd and 6f235e5: FOUND
