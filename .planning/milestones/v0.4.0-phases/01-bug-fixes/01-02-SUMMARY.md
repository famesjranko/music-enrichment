---
phase: 01-bug-fixes
plan: 02
subsystem: providers
tags: [lrclib, wikidata, duration, rank, bug-fix, tdd]

# Dependency graph
requires: []
provides:
  - "LRCLIB duration passed as Double preserving fractional seconds"
  - "Wikidata preferred-rank P18 claim selection"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Rank-aware claim selection in Wikidata API responses"
    - "Float division for millisecond-to-second conversion"

key-files:
  created: []
  modified:
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibApi.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataApi.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibProviderTest.kt"
    - "musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataProviderTest.kt"

key-decisions:
  - "Used Double parameter type for duration instead of String to leverage Kotlin string interpolation for float formatting"
  - "Preferred-rank fallback uses first claim (index 0) matching existing behavior when no preferred rank exists"

patterns-established:
  - "Rank-aware Wikidata claim selection: scan for preferred rank, fall back to first"

requirements-completed: [BUG-04, BUG-05]

# Metrics
duration: 4min
completed: 2026-03-21
---

# Phase 01 Plan 02: LRCLIB/Wikidata Bug Fixes Summary

**Fixed LRCLIB duration truncation (Int to Double) and Wikidata preferred-rank P18 claim selection, verified by 5 new TDD tests**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-21T07:19:33Z
- **Completed:** 2026-03-21T07:24:01Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- LRCLIB API now passes duration as Double, preserving fractional seconds (e.g., 238500ms becomes 238.5 in the URL instead of truncated 238)
- Wikidata extractImageFilename now selects preferred-rank P18 claims before falling back to first claim
- 5 new unit tests added across both providers, all following TDD RED-GREEN flow

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix LRCLIB duration truncation (BUG-04)** - `f646f72` (test: RED) + `76f0444` (fix: GREEN)
2. **Task 2: Fix Wikidata preferred-rank claim selection (BUG-05)** - `0cd8f5e` (test: RED) + `fd7b8a0` (fix: GREEN)

_Note: TDD tasks have two commits each (test then fix)_

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibApi.kt` - Changed durationSec parameter from Int? to Double?
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibProvider.kt` - Changed duration conversion from (it/1000).toInt() to it/1000.0
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataApi.kt` - Added rank-aware claim selection in extractImageFilename
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/lrclib/LrcLibProviderTest.kt` - Added 2 tests for float duration, updated 1 existing assertion
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataProviderTest.kt` - Added 3 tests for rank-aware claim selection

## Decisions Made
- Used Double parameter type for duration instead of String to leverage Kotlin's string interpolation producing float format (e.g., 238.0 not 238)
- Preferred-rank fallback uses first claim (index 0) matching existing behavior when no preferred rank exists, ensuring backward compatibility

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 5 known provider bugs now addressed (combined with plan 01-01)
- Provider internals ready for Phase 02 abstraction work

---
*Phase: 01-bug-fixes*
*Completed: 2026-03-21*

## Self-Check: PASSED
- All 6 files verified present
- All 4 commits verified in git log
