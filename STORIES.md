# musicmeta - Project Stories

> A living document of architectural decisions, progress, and lessons learned.
> Updated as the project evolves. Newest entries first within each section.

---

## Decisions

### 2026-03-21: Showcase demo and `runTest` discovery

**Context**: Built a comprehensive E2E showcase test to exercise every enrichment type across diverse queries (Radiohead, Kendrick Lamar, AC/DC, Bjork, instrumentals, obscure artists). The goal was both to demo capabilities and to find where the library needs improvement.

**Discovery — `runTest` breaks all E2E tests**: Every `engine.enrich()` call returned empty results under `runTest`. Root cause: `runTest` uses virtual time, and the engine's `withTimeout(30_000)` fires based on virtual time. Rate limiter `delay()` calls advance virtual time instantly, causing the timeout to fire before any real HTTP response arrives. Error message from kotlinx.coroutines confirms: *"Timed out after 30s of _virtual_ (kotlinx.coroutines.test) time."* All existing E2E tests (16 tests in `RealApiEndToEndTest`) were silently broken — they were passing only because the assertions happened to pass on empty results (they didn't). Switched all E2E tests to `runBlocking`.

**Discovery — identity resolution skipped wikidata/wikipedia for artists**: When the engine called MusicBrainz for identity resolution with type=GENRE, the provider's optimization (`type in RELATION_DEPENDENT_TYPES`) skipped the full lookup that gets wikidata/wikipedia URLs. This meant ARTIST_PHOTO (Wikidata) and ARTIST_BIO (Wikipedia) always failed for artist requests. Fixed by removing the type gate — always do the full lookup when the search result is missing wikidata/wikipedia.

**Discovery — `extractResolution` mismatch**: The existing E2E tests' `extractResolution` helper looked for `IdentifierResolution` data in results, but the engine stores `Metadata` data with `resolvedIdentifiers` attached to the Success wrapper. This was a stale pattern from before the engine was refactored. Fixed to reconstruct from current data model.

**Lesson**: E2E tests against real APIs should use `runBlocking`, not `runTest`. The virtual-time model in `runTest` is designed for unit tests with faked dependencies, not for integration tests with real I/O. The `RateLimiter` is particularly problematic because it uses `System.currentTimeMillis()` for the clock but `delay()` for waiting — a mismatch that causes artificial time accumulation under virtual time.

**Status**: Active

---

### 2026-03-21: Open-source readiness — remove opinionation

**Context**: musicmeta was extracted from Cascade (Android music player) as a standalone library. A review found several hardcoded values and Cascade-specific assumptions that would limit adoption by other apps.

**Changes**:
- Provider chain priorities made configurable via `EnrichmentConfig.priorityOverrides`. Apps can reorder which provider is tried first for each enrichment type without modifying provider code.
- Artwork sizes moved from engine-level config to per-provider constructor params. Each API has different size semantics (CAA uses pixel sizes in URLs, iTunes uses string replacement, Wikidata uses width params), so a single "preferred size" in config was misleading — it wasn't wired to anything.
- MusicBrainz `minMatchScore` (was hardcoded at 80) now a constructor param. Apps with obscure catalogs or non-Latin scripts can lower it.
- `ArtistMatcher.isMatch()` token overlap threshold now configurable (was hardcoded 0.5).
- Room database name extracted to a public constant so apps know what to change.
- All silent `catch (_: Exception)` blocks now log through `EnrichmentLogger`.
- Removed all Cascade references from test User-Agent strings.

**Rationale**: A library should let consumers tune behavior without forking. Constructor params with sensible defaults preserve backwards compatibility while opening up flexibility.

**Status**: Active

---

### 2026-03-21: Extraction from Cascade

**Context**: Cascade's metadata enrichment started as in-app API clients (MusicBrainz, Wikidata, Cover Art Archive). As the provider count grew to 11, the enrichment logic outgrew the app's data layer. The engine also has value as a standalone open-source library.

**Decision**: Extract into a two-module library:
- `musicmeta-core` (pure Kotlin/JVM) — engine, providers, HTTP, caching interface. Zero Android dependencies.
- `musicmeta-android` (optional) — Room cache, Hilt DI wiring, WorkManager base worker.

**Architecture**:
- `EnrichmentEngine` orchestrates the pipeline: cache check → identity resolution → fan-out to provider chains → confidence filtering → cache store.
- `ProviderRegistry` builds a `ProviderChain` per `EnrichmentType`, ordered by `ProviderCapability.priority`. The chain tries providers in order; `Success` short-circuits, `NotFound` falls through.
- Each provider is self-contained: own `*Api.kt` (HTTP), `*Models.kt` (parsing), `*Provider.kt` (enrichment logic). Adding a provider means implementing `EnrichmentProvider` — no engine changes needed.
- `CircuitBreaker` per provider (shared across chains) protects against cascading failures from a single degraded API.

**Trade-offs**:
- **Pro**: Reusable across apps, testable in isolation, pure Kotlin core runs on any JVM
- **Pro**: Provider architecture is open/closed — add new sources without modifying engine
- **Con**: Cascade's data layer now needs a mapping layer between engine types and domain models
- **Con**: JitPack dependency for consumers (vs composite build alternative)

**Key design choice — MusicBrainz as identity backbone**: MusicBrainz resolves MBIDs, Wikidata IDs, and Wikipedia titles. Downstream providers (Cover Art Archive, Wikidata, Wikipedia, Fanart.tv) use these IDs for precise lookups instead of fuzzy search. This dramatically improves accuracy but means MusicBrainz is a soft dependency. `enableIdentityResolution = false` provides an escape hatch.

**Status**: Active
