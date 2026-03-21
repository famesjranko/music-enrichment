---
phase: 10-genre-enhancement
plan: 01
subsystem: engine
tags: [genre, enrichment-data, serialization, pure-function, kotlin]

# Dependency graph
requires:
  - phase: 09-artist-timeline
    provides: TimelineEvent top-level @Serializable pattern used by GenreTag
provides:
  - GenreTag @Serializable data class with name, confidence, sources
  - genreTags field on Metadata (backward-compatible alongside genres)
  - GenreMerger.merge() pure function in engine/ package
  - normalize() function with ALIASES map for common genre variants
affects: [10-genre-enhancement/10-02, 10-genre-enhancement/10-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - GenreTag as top-level @Serializable data class, consistent with TimelineEvent/BandMember pattern
    - GenreMerger as object with pure merge() — no side effects, no dependencies on engine state

key-files:
  created:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/GenreMerger.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/GenreMergerTest.kt
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/EnrichmentDataSerializationTest.kt

key-decisions:
  - "GenreTag placed as top-level @Serializable class in EnrichmentData.kt, consistent with TimelineEvent/BandMember/Credit pattern"
  - "Display name uses first-seen casing (not normalized lowercase) — normalized key used only for grouping/deduplication"
  - "ALIASES map covers common genre variants: alt rock, hip hop, hiphop, rnb, r & b, electronica, synth pop, post punk"
  - "GenreMerger.normalize() marked internal to allow direct testing from same-package tests"

patterns-established:
  - "Pure merge function as object: stateless, deterministic, easily testable"
  - "Confidence capping with coerceAtMost(1.0f) — standard Float capping idiom"
  - "LinkedHashMap for grouped processing preserves insertion order for first-seen display name"

requirements-completed: [GENR-01, GENR-02]

# Metrics
duration: 3min
completed: 2026-03-22
---

# Phase 10 Plan 01: Genre Enhancement - GenreTag Data Model and GenreMerger Summary

**GenreTag @Serializable data class with confidence + sources, Metadata.genreTags field, and GenreMerger.merge() with alias normalization, deduplication, and additive confidence scoring**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T17:44:02Z
- **Completed:** 2026-03-21T17:47:04Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Added `GenreTag` top-level @Serializable data class (name, confidence, sources) consistent with TimelineEvent/BandMember pattern
- Added `genreTags: List<GenreTag>? = null` field to `Metadata` immediately after `genres` — fully backward compatible
- Implemented `GenreMerger.merge()` pure function: normalizes names, maps aliases, deduplicates by normalized key with additive confidence (capped at 1.0), sorts descending
- 8 GenreMergerTest cases cover empty input, lowercase normalization, alias mapping, deduplication, confidence capping, sorting, first-seen display name, and single provider input

## Task Commits

Each task was committed atomically:

1. **Task 1: GenreTag data class + genreTags field on Metadata** - `4640dc8` (feat)
2. **Task 2: RED — GenreMerger failing tests** - `43b66b3` (test)
3. **Task 2: GREEN — GenreMerger implementation** - `a83d2c7` (feat)

_Note: TDD task 2 has two commits (test → feat)_

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt` - Added GenreTag top-level class and genreTags field on Metadata
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/GenreMerger.kt` - New: pure merge function with ALIASES map and normalize()
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/GenreMergerTest.kt` - New: 8 TDD tests for GenreMerger
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/EnrichmentDataSerializationTest.kt` - Added GenreTag and Metadata+genreTags round-trip tests

## Decisions Made
- Display name preserves first-seen casing (not lowercased) — normalization is only for the grouping key. This matches the plan spec: "Output name uses the first-seen casing (the one from the highest-priority provider)."
- `normalize()` is `internal` rather than `private` to allow direct testing from the same package in tests.
- ALIASES map is private to GenreMerger — callers only need `merge()` and the aliases are an implementation detail.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed incorrect test expectation for display name casing**
- **Found during:** Task 2 (GenreMerger GREEN phase)
- **Issue:** The `merge handles single provider input` test expected `"electronic"` (lowercase) but the behavior spec says "Output name uses the first-seen casing". The input was `GenreTag(name = "Electronic", ...)` so the output should be `"Electronic"`.
- **Fix:** Changed test assertions to expect first-seen casing (`"Electronic"`, `"Ambient"`) with comment clarifying the behavior.
- **Files modified:** GenreMergerTest.kt
- **Verification:** All 8 tests pass after fix.
- **Committed in:** a83d2c7 (Task 2 GREEN commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - test bug)
**Impact on plan:** Minimal — fixed a test assertion that contradicted the plan's behavior spec. Implementation unchanged.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- GenreTag and GenreMerger are the data contracts and merge logic needed by Plans 02 and 03
- Plan 02 will add provider mappings to emit GenreTag lists
- Plan 03 will wire multi-provider genre merging into the engine

---
*Phase: 10-genre-enhancement*
*Completed: 2026-03-22*

## Self-Check: PASSED

All files found and all commits verified:
- FOUND: EnrichmentData.kt
- FOUND: GenreMerger.kt
- FOUND: GenreMergerTest.kt
- FOUND: 10-01-SUMMARY.md
- FOUND: commit 4640dc8 (feat: GenreTag data class and genreTags field)
- FOUND: commit 43b66b3 (test: GenreMerger failing tests)
- FOUND: commit a83d2c7 (feat: GenreMerger implementation)
