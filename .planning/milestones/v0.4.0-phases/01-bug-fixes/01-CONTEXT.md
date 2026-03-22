# Phase 1: Bug Fixes - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix 5 known provider bugs before structural changes touch the same files. Pure correctness fixes — no new features, no API changes.

</domain>

<decisions>
## Implementation Decisions

### Claude's Discretion
All implementation choices are at Claude's discretion — pure infrastructure phase.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FakeProvider`, `FakeHttpClient`, `FakeEnrichmentCache` in `testutil/` for unit tests
- `kotlinx.coroutines.test.runTest` for coroutine testing

### Established Patterns
- Tests use backtick-style names: `` `provider returns NotFound when album has no art` ``
- Given-When-Then structure with comments
- E2E tests gated by `-Dinclude.e2e=true`

### Integration Points
- `MusicBrainzProvider.kt` lines 129, 170, 200 — empty results → RateLimited (should be NotFound)
- `LastFmApi.kt` line 80 — `http://` BASE_URL (should be `https://`)
- `LastFmProvider.kt` line 39 — TRACK_POPULARITY capability declared but only artist-level data returned
- `LrcLibProvider.kt` line 61 — `(it / 1000).toInt()` truncates duration (should be float)
- `WikidataApi.kt` line 31 — blindly takes `[0]` without rank filtering

</code_context>

<specifics>
## Specific Ideas

No specific requirements — infrastructure phase.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>
