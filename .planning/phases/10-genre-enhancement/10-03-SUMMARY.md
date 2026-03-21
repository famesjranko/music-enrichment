---
phase: 10-genre-enhancement
plan: "03"
subsystem: engine
tags: [genre, merger, provider-chain, fan-out]
dependency_graph:
  requires: [10-01, 10-02]
  provides: [GENR-02, GENR-03, GENR-04]
  affects: [DefaultEnrichmentEngine, ProviderChain]
tech_stack:
  added: []
  patterns: [resolve-all-collect, mergeable-type-routing, confidence-merge]
key_files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/ProviderChain.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngine.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/ProviderChainTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngineTest.kt
decisions:
  - "resolveAll() placed on ProviderChain alongside resolve() — structurally identical except collects into list instead of early return"
  - "MERGEABLE_TYPES companion set in DefaultEnrichmentEngine — allows future types beyond GENRE with zero engine changes"
  - "mergeGenreResults() falls back to first-success when no genreTags present — graceful degradation for providers that predate genreTags"
  - "Confidence filter applied per-result before merge — consistent with existing filterByConfidence path"
  - "GENRE in IDENTITY_TYPES and MERGEABLE_TYPES simultaneously — when identity resolution handles GENRE it is removed from uncachedTypes before resolveTypes sees it; no conflict"
metrics:
  duration: "174s"
  completed: "2026-03-22"
  tasks: 2
  files: 4
---

# Phase 10 Plan 03: Mergeable Type Wiring Summary

Multi-provider genre merging wired into the engine pipeline via `ProviderChain.resolveAll()` and `DefaultEnrichmentEngine` MERGEABLE_TYPES routing. GENRE now collects results from all capable providers and merges them through GenreMerger.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | ProviderChain.resolveAll() for mergeable types | 2a66579 | ProviderChain.kt, ProviderChainTest.kt |
| 2 | Engine mergeable type wiring with GenreMerger integration | c9aab56 | DefaultEnrichmentEngine.kt, DefaultEnrichmentEngineTest.kt |

## What Was Built

**Task 1 — ProviderChain.resolveAll():** New method that iterates all providers without short-circuiting on first success. Returns `List<EnrichmentResult.Success>` (empty if no providers succeed). Respects all existing guards: availability check, identifier requirement check, circuit breaker check. Error handling identical to `resolve()`.

**Task 2 — Engine merge wiring:** `resolveTypes()` now splits standard types into mergeable vs regular. GENRE routes through `resolveAll() + mergeGenreResults()`. The merge function collects all `genreTags` from provider results, calls `GenreMerger.merge()`, and builds a synthetic `Success` result with merged `genreTags` (deduplicated, confidence-summed, sorted) and a backward-compatible `genres` list (top 10 names). Provider field is `"genre_merger"`, confidence is max from collected results.

## Decisions Made

- `resolveAll()` placed on `ProviderChain` alongside `resolve()` — structurally identical except collects into list instead of early return
- `MERGEABLE_TYPES` companion set in `DefaultEnrichmentEngine` — allows future types beyond GENRE with zero engine changes
- `mergeGenreResults()` falls back to first-success when no `genreTags` present — graceful degradation for providers that predate genreTags
- Confidence filter applied per-result before merge — consistent with existing `filterByConfidence` path
- GENRE in `IDENTITY_TYPES` and `MERGEABLE_TYPES` simultaneously — when identity resolution handles GENRE it is removed from `uncachedTypes` before `resolveTypes` sees it; no conflict

## Tests Added

**ProviderChainTest.kt — 5 new tests:**
- `resolveAll collects results from all providers`
- `resolveAll skips NotFound and continues`
- `resolveAll returns empty list when all providers fail`
- `resolveAll respects circuit breakers`
- `resolveAll skips unavailable providers`

**DefaultEnrichmentEngineTest.kt — 4 new tests:**
- `GENRE type merges results from multiple providers`
- `GENRE merged result populates backward-compatible genres list`
- `GENRE merge uses genre_merger as provider`
- `non-GENRE types still short-circuit on first success`

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — all genre tag data paths are wired end to end.

## Self-Check: PASSED

Files created/modified exist:
- musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/ProviderChain.kt: FOUND
- musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngine.kt: FOUND
- musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/ProviderChainTest.kt: FOUND
- musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngineTest.kt: FOUND

Commits verified:
- 2a66579: feat(10-03): add ProviderChain.resolveAll() for mergeable types
- c9aab56: feat(10-03): wire GenreMerger into engine for mergeable GENRE type
