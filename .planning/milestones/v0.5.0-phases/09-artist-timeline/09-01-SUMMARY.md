---
phase: 09-artist-timeline
plan: "01"
subsystem: enrichment-types
tags: [kotlin, serialization, timeline, synthesis, tdd]

# Dependency graph
requires:
  - phase: 08-release-editions
    provides: EnrichmentData sealed class, EnrichmentType enum, EnrichmentResult patterns
  - phase: 07-credits-personnel
    provides: BandMembers, Discography sub-types used as synthesizer inputs
provides:
  - ARTIST_TIMELINE EnrichmentType enum value (30-day TTL)
  - ArtistTimeline data class inside EnrichmentData sealed class
  - TimelineEvent top-level @Serializable data class
  - TimelineSynthesizer pure object with synthesize() function
  - 16 TDD tests covering all event types, sorting, deduplication, graceful degradation
affects: [09-02-PLAN, DefaultEnrichmentEngine, ProviderRegistry]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Pure object synthesizer pattern: TimelineSynthesizer takes EnrichmentResult? params, returns data class
    - Graceful degradation: null means not-requested, NotFound means searched-but-empty, both handled identically in extraction helpers

key-files:
  created:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/TimelineSynthesizer.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/TimelineSynthesizerTest.kt
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt

key-decisions:
  - "TimelineSynthesizer.synthesize() accepts EnrichmentResult? (not EnrichmentData?) so callers pass raw engine results without unwrapping"
  - "null artistType defaults to Group behavior (formed/disbanded) — most artists in MusicBrainz without explicit type are groups"
  - "TimelineEvent placed as top-level @Serializable class (not nested inside EnrichmentData) — consistent with BandMember, DiscographyAlbum, etc."
  - "extractDiscographyEvents: albums without any year value still emit album_release events with empty date string rather than being silently dropped"

patterns-established:
  - "Synthesizer pure function pattern: mutable list built by private extraction helpers, then sorted + deduplicated before return"
  - "Deduplication key: date:type compound string via distinctBy — preserves first occurrence on tie"

requirements-completed: [TIME-01, TIME-02]

# Metrics
duration: 2min
completed: 2026-03-22
---

# Phase 09 Plan 01: Artist Timeline Data Model + Synthesizer Summary

**ARTIST_TIMELINE EnrichmentType with ArtistTimeline/TimelineEvent data classes and a pure TimelineSynthesizer that produces chronologically sorted, deduplicated timeline events from life-span metadata, discography albums, and band member changes**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-21T17:21:49Z
- **Completed:** 2026-03-22T17:24:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Defined ARTIST_TIMELINE enum value with 30-day TTL in a new "// Composite" section
- Added ArtistTimeline(events: List<TimelineEvent>) inside EnrichmentData sealed class and TimelineEvent as top-level @Serializable data class
- Implemented TimelineSynthesizer pure object with three private extraction helpers (life-span, discography, band members), sort, and deduplication
- 16 TDD tests covering: born/died/formed/disbanded event types, first_album marking, member_joined/left parsing, chronological sort, deduplication, partial timelines, null inputs, null artistType default

## Task Commits

Each task was committed atomically:

1. **Task 1: Define ARTIST_TIMELINE type + ArtistTimeline/TimelineEvent data model** - `0feb7f6` (feat)
2. **Task 2: Implement TimelineSynthesizer with chronological event synthesis** - `55c72a6` (feat)

## Files Created/Modified
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentType.kt` - Added ARTIST_TIMELINE(30-day TTL) in Composite section
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentData.kt` - Added ArtistTimeline data class (nested) and TimelineEvent (top-level)
- `musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/TimelineSynthesizer.kt` - Created pure synthesizer object
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/TimelineSynthesizerTest.kt` - Created 16 TDD tests
- `musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/e2e/EnrichmentShowcaseTest.kt` - Added ArtistTimeline branch to exhaustive when in snippet()

## Decisions Made
- TimelineSynthesizer.synthesize() accepts EnrichmentResult? so callers pass raw results without unwrapping — simplifies Plan 02 integration in DefaultEnrichmentEngine
- null artistType defaults to Group behavior (formed/disbanded) — most MusicBrainz artists without explicit type are groups
- TimelineEvent placed as top-level @Serializable class, consistent with BandMember, DiscographyAlbum, etc.
- Albums without year values still emit events with empty date string rather than being silently dropped

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All data contracts established: ARTIST_TIMELINE type, ArtistTimeline/TimelineEvent data classes, TimelineSynthesizer synthesize() function
- Plan 02 can wire TimelineSynthesizer into DefaultEnrichmentEngine: detect ARTIST_TIMELINE request, fan-out to ARTIST_METADATA + ARTIST_DISCOGRAPHY + BAND_MEMBERS, pass results to synthesize(), return as Success
- No blockers

---
*Phase: 09-artist-timeline*
*Completed: 2026-03-22*
