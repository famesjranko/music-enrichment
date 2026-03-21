# Phase 2: Provider Abstraction - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Isolate provider internals behind a typed abstraction layer. Six sub-features: typed identifier requirements, identity provider formalization, mapper pattern extraction, API key centralization, data-driven needsIdentityResolution, and removing IdentifierResolution from public API.

</domain>

<decisions>
## Implementation Decisions

### Claude's Discretion
All implementation choices are at Claude's discretion — pure infrastructure phase.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ProviderCapability` data class in `EnrichmentProvider.kt` — needs `identifierRequirement` field
- `ProviderChain.hasRequiredIdentifiers()` — needs typed identifier checking
- `ProviderRegistry.identityProvider()` — needs `isIdentityProvider` flag
- 11 provider directories each with `*Api.kt`, `*Models.kt`, `*Provider.kt`
- Test fakes: `FakeProvider`, `FakeHttpClient` in `testutil/`

### Established Patterns
- Provider structure: Api (HTTP) → Models (DTOs) → Provider (enrichment logic)
- `ProviderCapability(type, priority, requiresIdentifier)` — boolean needs replacing with enum
- Identity resolution hardcoded in `DefaultEnrichmentEngine` with `IDENTITY_TYPES` set
- `EnrichmentData.IdentifierResolution` is public sealed subclass used only internally

### Integration Points
- `EnrichmentProvider.kt` — interface all 11 providers implement
- `ProviderChain.kt` — uses `hasRequiredIdentifiers()` per-provider
- `ProviderRegistry.kt` — `identityProvider()` selects by GENRE/LABEL heuristic
- `DefaultEnrichmentEngine.kt` — `IDENTITY_TYPES`, `needsIdentityResolution()`, `resolveIdentity()`
- `EnrichmentData.kt` — `IdentifierResolution` sealed subclass to remove
- `EnrichmentEngine.kt` — Builder needs `apiKeys()` and `withDefaultProviders()`

</code_context>

<specifics>
## Specific Ideas

No specific requirements — infrastructure phase.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>
