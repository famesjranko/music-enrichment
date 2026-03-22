# Phase 3: Public API Cleanup - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Clean up public types: move TTL into EnrichmentType enum, add extensible identifiers via extra map, clean provider leaks from EnrichmentData, add error categorization, and add typed HttpResult for precise HTTP response handling.

</domain>

<decisions>
## Implementation Decisions

### Claude's Discretion
All implementation choices are at Claude's discretion — pure infrastructure phase.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EnrichmentType` enum — needs constructor params (defaultTtlMs, category)
- `EnrichmentIdentifiers` data class — needs `extra: Map<String, String>` field
- `EnrichmentData.SimilarArtist` and `PopularTrack` — have bare `musicBrainzId: String?`
- `EnrichmentResult.Error` — needs `errorKind` field
- `HttpClient` interface — needs `fetchJsonResult()` returning `HttpResult<T>`
- `DefaultEnrichmentEngine.ttlFor()` — static when-block to remove

### Established Patterns
- Phase 2 established mapper pattern — all EnrichmentData construction is in mappers
- `ProviderCapability` now has typed `identifierRequirement`
- `EnrichmentResult.Success` now has `resolvedIdentifiers` field

### Integration Points
- `EnrichmentType.kt` — enum needs TTL + category constructor params
- `EnrichmentConfig.kt` — needs `ttlOverrides` map
- `DefaultEnrichmentEngine.kt` — remove `ttlFor()` companion
- `EnrichmentRequest.kt` / `EnrichmentIdentifiers` — add extra map + accessor
- `EnrichmentData.kt` — clean SimilarArtist/PopularTrack
- `EnrichmentResult.kt` — add ErrorKind
- `HttpClient.kt` / `DefaultHttpClient.kt` — add HttpResult sealed class

</code_context>

<specifics>
## Specific Ideas

No specific requirements — infrastructure phase.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>
