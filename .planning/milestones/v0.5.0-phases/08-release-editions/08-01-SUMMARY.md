---
phase: 08-release-editions
plan: "01"
subsystem: musicmeta-core
tags: [enrichment-type, musicbrainz, release-editions, tdd]
dependency_graph:
  requires: [07-credits-personnel/07-01, 07-credits-personnel/07-02]
  provides: [RELEASE_EDITIONS-type, ReleaseEditions-data, MusicBrainzReleaseGroup-api]
  affects: [EnrichmentType, EnrichmentData, MusicBrainzProvider, MusicBrainzApi, MusicBrainzParser, MusicBrainzMapper]
tech_stack:
  added: []
  patterns: [tdd-red-green, mapper-pattern, id-based-lookup, type-dispatch-guard]
key_files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzModels.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParser.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzMapper.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzParserTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/musicbrainz/MusicBrainzProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt
decisions:
  - "lookupReleaseGroup returns raw JSONObject (same pattern as lookupRecording) — release sub-structures are complex and only needed for editions"
  - "RELEASE_EDITIONS dispatch guard placed after ALBUM_TRACKS guard in enrichAlbum, consistent with existing pattern"
  - "parseReleaseGroupDetail extracts format from media[0].format and label/catalogNumber from label-info[0] — first medium and first label only"
  - "enrichAlbumEditions returns NotFound for empty releases list (same pattern as enrichAlbumTracks for empty tracks)"
metrics:
  duration_minutes: 4
  tasks_completed: 2
  files_modified: 10
  completed_date: "2026-03-22"
---

# Phase 08 Plan 01: Release Editions - MusicBrainz Provider Summary

**One-liner:** RELEASE_EDITIONS enrichment type (1-year TTL) with MusicBrainz release-group endpoint at priority 100, returning per-edition format/country/year/label/catalogNumber/barcode via ReleaseEditions data class.

## What Was Built

Added the RELEASE_EDITIONS enrichment type end-to-end through the MusicBrainz provider stack:

1. **EnrichmentType.RELEASE_EDITIONS** — added with 365-day TTL in the "Additional metadata" section after ALBUM_TRACKS. Editions rarely change so 1-year TTL is appropriate.

2. **EnrichmentData.ReleaseEditions / ReleaseEdition** — both `@Serializable`. `ReleaseEditions` holds a list of `ReleaseEdition` items. `ReleaseEdition` has: title, format, country, year (Int), label, catalogNumber, barcode, identifiers (carrying musicBrainzId for the individual release).

3. **MusicBrainzModels.kt** — added `MusicBrainzReleaseGroupDetail` (id, title, releases) and `MusicBrainzEdition` (id, title, date, country, barcode, format, label, catalogNumber).

4. **MusicBrainzApi.lookupReleaseGroup** — fetches `/ws/2/release-group/{mbid}?fmt=json&inc=releases+labels+media`. Returns raw JSONObject following the same pattern as `lookupRecording`.

5. **MusicBrainzParser.parseReleaseGroupDetail** — extracts releases array; for each release extracts format from `media[0].format`, label from `label-info[0].label.name`, catalog number from `label-info[0].catalog-number`, plus date, country, barcode.

6. **MusicBrainzMapper.toReleaseEditions** — maps `MusicBrainzReleaseGroupDetail` to `EnrichmentData.ReleaseEditions`. Year extracted as `date?.take(4)?.toIntOrNull()` from full ISO date strings.

7. **MusicBrainzProvider** — RELEASE_EDITIONS capability at priority 100 with `MUSICBRAINZ_RELEASE_GROUP_ID` requirement. Dispatch guard added after ALBUM_TRACKS guard. `enrichAlbumEditions` reads release-group MBID from identifiers, calls API, parses, maps, returns Success with `ConfidenceCalculator.idBasedLookup()` (1.0f).

8. **EnrichmentShowcaseTest** — added `is EnrichmentData.ReleaseEditions -> "${data.editions.size} editions"` branch to the exhaustive `when` in `snippet()`.

## Tests Added

**MusicBrainzParserTest (5 new tests):**
- `parseReleaseGroupDetail parses releases array into MusicBrainzReleaseGroupDetail`
- `parseReleaseGroupDetail extracts all fields from release object`
- `parseReleaseGroupDetail returns empty releases list when releases array absent`
- `toReleaseEditions maps MusicBrainzEdition list to ReleaseEditions with correct fields`
- `ReleaseEditions round-trip serialization works`

**MusicBrainzProviderTest (5 new tests):**
- `provider has RELEASE_EDITIONS capability at priority 100 with MUSICBRAINZ_RELEASE_GROUP_ID requirement`
- `enrichAlbumEditions returns Success with ReleaseEditions when release-group has releases`
- `enrichAlbumEditions returns NotFound when release-group MBID missing from identifiers`
- `enrichAlbumEditions returns NotFound when lookupReleaseGroup returns null`
- `enrichAlbumEditions returns Error with NETWORK ErrorKind on IOException`

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 3003bf4 | feat(08-01): add RELEASE_EDITIONS type, data model, MusicBrainz API/Parser/Mapper |
| 2 | 06ffa87 | feat(08-01): wire MusicBrainz RELEASE_EDITIONS capability into Provider |

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - all data fields are wired from the MusicBrainz API response. The `enrichAlbumEditions` function returns real data from `/ws/2/release-group/{mbid}?fmt=json&inc=releases+labels+media`.

## Self-Check: PASSED
