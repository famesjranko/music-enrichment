# musicmeta PRD: Production-Grade Music Metadata Library

## Context

musicmeta is at v0.1.0 with 11 providers, 16 enrichment types, and a solid architecture (priority chains, circuit breakers, rate limiting, identity resolution). The core problem: **public APIs we depend on are the biggest long-term maintenance risk**. Provider APIs change, break, deprecate, and rate-limit differently. The library needs abstraction layers that isolate these risks from consumers and make provider changes easy.

The current architecture has provider-specific data leaking into public types, identity resolution hardcoded to MusicBrainz patterns, and rigid types that require changes in many places when extended. Since we're pre-1.0 with no external consumers, we can make clean breaking changes now to get the API surface right.

This PRD covers: abstraction layer overhaul, bug fixes, all ROADMAP Phase 1 new types (6 types + artwork sizes), and ROADMAP Phase 2 deepening.

---

## Phase 0: Bug Fixes (~1 day)

Fix known bugs from AUDIT.md before structural work touches the same files.

### 0.1 MusicBrainz: Empty results misclassified as RateLimited
- **File:** `provider/musicbrainz/MusicBrainzProvider.kt` (lines ~129, 170, 200)
- **Fix:** Change `EnrichmentResult.RateLimited` to `EnrichmentResult.NotFound` for empty search results. MusicBrainz uses HTTP 503 for rate limiting (handled by `DefaultHttpClient`), not empty results.

### 0.2 Last.fm: HTTP to HTTPS
- **File:** `provider/lastfm/LastFmApi.kt` (line ~80)
- **Fix:** `http://ws.audioscrobbler.com/2.0/` -> `https://ws.audioscrobbler.com/2.0/`

### 0.3 Last.fm: TRACK_POPULARITY returns artist-level data
- **File:** `provider/lastfm/LastFmProvider.kt`
- **Fix:** Remove `TRACK_POPULARITY` from capabilities until `track.getInfo` is wired (Phase 4). Current `enrichPopularity()` calls `artist.getinfo` which returns artist-level data.

### 0.4 LRCLIB: Duration precision loss
- **File:** `provider/lrclib/LrcLibProvider.kt` (line ~61)
- **Fix:** `(it / 1000).toInt()` -> pass as float/double. LRCLIB API accepts float durations.

### 0.5 Wikidata: Claim rank handling
- **File:** `provider/wikidata/WikidataApi.kt` (line ~31)
- **Fix:** Filter for `"rank": "preferred"` first, then `"rank": "normal"`, instead of blindly taking `[0]`.

### Verification
- Update unit test assertions (e.g., expect `NotFound` instead of `RateLimited` for empty MB results)
- Run full test suite: `./gradlew :musicmeta-core:test`
- Run E2E: `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

---

## Phase 1: Provider Abstraction Layer (~1-2 weeks)

**Goal:** Isolate provider internals from the public API. This is the highest-leverage structural change.

### 1.1 Typed Identifier Requirements

**Problem:** `ProviderCapability.requiresIdentifier: Boolean` is too coarse. `ProviderChain.hasRequiredIdentifiers()` checks if ANY identifier exists, but WikidataProvider needs `wikidataId`, CoverArtArchiveProvider needs `musicBrainzId`, WikipediaProvider needs `wikipediaTitle` or `wikidataId`.

**Change `EnrichmentProvider.kt`:**
```kotlin
enum class IdentifierRequirement {
    NONE,
    MUSICBRAINZ_ID,         // musicBrainzId or musicBrainzReleaseGroupId
    WIKIDATA_ID,
    WIKIPEDIA_TITLE,
    WIKIDATA_OR_WIKIPEDIA,
    ANY_RESOLVED_ID,
}

data class ProviderCapability(
    val type: EnrichmentType,
    val priority: Int,
    val identifierRequirement: IdentifierRequirement = IdentifierRequirement.NONE,
)
```

**Update `ProviderChain.hasRequiredIdentifiers()`** to check specific requirements.

**Update each provider's capabilities** to declare precise requirements:
- WikidataProvider: `WIKIDATA_ID`
- CoverArtArchiveProvider: `MUSICBRAINZ_ID`
- WikipediaProvider: `WIKIDATA_OR_WIKIPEDIA`
- FanartTvProvider: `MUSICBRAINZ_ID`
- etc.

### 1.2 Formalize Identity Resolution as a Provider Role

**Problem:** `ProviderRegistry.identityProvider()` selects by checking GENRE/LABEL capabilities (fragile heuristic). `IDENTITY_TYPES` and `needsIdentityResolution()` in `DefaultEnrichmentEngine` encode MusicBrainz-specific knowledge.

**Add to `EnrichmentProvider` interface:**
```kotlin
interface EnrichmentProvider {
    // ... existing ...
    val isIdentityProvider: Boolean get() = false
    val identityTypes: Set<EnrichmentType> get() = emptySet()
    suspend fun resolveIdentity(request: EnrichmentRequest): IdentityResolutionResult? = null
}
```

**New internal type** (`engine/IdentityResolutionResult.kt`):
```kotlin
data class IdentityResolutionResult(
    val identifiers: EnrichmentIdentifiers,
    val score: Int = 0,
    val metadata: EnrichmentData.Metadata? = null,
    val hasFrontCover: Boolean = false,
)
```

**Update `MusicBrainzProvider`:**
- `isIdentityProvider = true`
- `identityTypes = setOf(GENRE, LABEL, RELEASE_DATE, RELEASE_TYPE, COUNTRY)`
- Implement `resolveIdentity()` using existing resolution logic

**Refactor `DefaultEnrichmentEngine.resolveIdentity()`:**
- Call `provider.resolveIdentity(request)` instead of `provider.enrich(request, identityType)`
- Remove `IDENTITY_TYPES` hardcoded set, use `provider.identityTypes`
- Remove the `IdentifierResolution` type-check in the engine

**Remove `EnrichmentData.IdentifierResolution`** from the public sealed class (clean break, no deprecation).

### 1.3 Provider Mapper Pattern

**Problem:** Providers directly construct `EnrichmentData` subclasses, coupling provider code to public API shape. When the data model changes, every provider changes.

**New pattern:** Extract mapping logic from each Provider into a `*Mapper.kt` file:

```
provider/deezer/
  DeezerApi.kt        -- HTTP calls, returns internal DTOs
  DeezerModels.kt     -- Internal DTOs (unchanged)
  DeezerMapper.kt     -- NEW: maps DTOs -> EnrichmentData
  DeezerProvider.kt   -- Orchestrates: Api -> Mapper -> EnrichmentResult
```

**Create 11 mapper files**, one per provider. This is extracting existing code, not writing new logic. The value: when `Artwork` gains a `sizes` field, only mappers change.

### 1.4 Centralize API Key Management

**Problem:** API keys handled inconsistently (constructor lambdas vs nothing). No centralized config.

**New `ApiKeyConfig.kt`:**
```kotlin
data class ApiKeyConfig(
    val lastFmApiKey: String? = null,
    val fanartTvProjectKey: String? = null,
    val discogsToken: String? = null,
)
```

**Add to `EnrichmentEngine.Builder`:**
```kotlin
fun apiKeys(config: ApiKeyConfig) = apply { ... }
fun withDefaultProviders(httpClient: HttpClient) = apply {
    // Auto-construct all providers using apiKeys + httpClient
}
```

Existing per-provider constructor approach remains supported for custom wiring.

### 1.5 Improve `needsIdentityResolution()` Logic

**Problem:** `needsIdentityResolution()` hardcodes checks for specific types (`ARTIST_PHOTO`, `ARTIST_BIO`). Should be derived from provider capabilities.

**New logic:** Check if any requested type's chain contains a provider with `identifierRequirement != NONE` that lacks the required identifier. This is data-driven rather than hardcoded.

### Verification
- All existing unit tests pass with refactored code
- `ProviderChainTest`: new tests for precise identifier matching
- `ProviderRegistryTest`: `isIdentityProvider` selection
- `DefaultEnrichmentEngineTest`: new identity resolution pathway
- E2E tests unchanged behavior

---

## Phase 2: Public API Surface Cleanup (~1 week)

**Goal:** Make public types clean, extensible, and free of provider-specific leaks. Clean break since no external consumers.

### 2.1 Move TTL Into `EnrichmentType`

**Problem:** `DefaultEnrichmentEngine.ttlFor()` is a `when` that must be updated for every new type.

**Change `EnrichmentType.kt`:**
```kotlin
enum class EnrichmentType(val defaultTtlMs: Long, val category: Category) {
    ALBUM_ART(DAYS_90, ARTWORK),
    ARTIST_PHOTO(DAYS_30, ARTWORK),
    GENRE(DAYS_90, METADATA),
    // ... etc

    enum class Category { ARTWORK, METADATA, TEXT, RELATIONSHIPS, STATISTICS }
}
```

**Add TTL override to `EnrichmentConfig`:**
```kotlin
val ttlOverrides: Map<EnrichmentType, Long> = emptyMap()
```

**Remove `ttlFor()` companion function** from `DefaultEnrichmentEngine`. Replace with:
```kotlin
config.ttlOverrides[type] ?: type.defaultTtlMs
```

### 2.2 Extensible `EnrichmentIdentifiers`

**Problem:** Adding new identifier types (Deezer ID, Discogs ID, Spotify URI) requires changing the data class.

**Add an `extra` map:**
```kotlin
data class EnrichmentIdentifiers(
    val musicBrainzId: String? = null,
    val musicBrainzReleaseGroupId: String? = null,
    val wikidataId: String? = null,
    val isrc: String? = null,
    val barcode: String? = null,
    val wikipediaTitle: String? = null,
    val extra: Map<String, String> = emptyMap(),
) {
    fun get(key: String): String? = when(key) {
        "musicBrainzId" -> musicBrainzId
        // ...typed lookups...
        else -> extra[key]
    }
    fun withExtra(key: String, value: String) = copy(extra = extra + (key to value))
}
```

### 2.3 Clean Provider Leaks from `EnrichmentData`

**`SimilarArtist` and `PopularTrack`:** Replace `musicBrainzId: String?` with `identifiers: EnrichmentIdentifiers`:
```kotlin
data class SimilarArtist(
    val name: String,
    val matchScore: Float,
    val identifiers: EnrichmentIdentifiers = EnrichmentIdentifiers(),
)

data class PopularTrack(
    val title: String,
    val listenCount: Long,
    val rank: Int,
    val identifiers: EnrichmentIdentifiers = EnrichmentIdentifiers(),
)
```

**`EnrichmentData.Metadata` fields** (`barcode`, `artistType`, `beginDate`, `endDate`, `disambiguation`): These are actually general-purpose music metadata concepts, not MusicBrainz-specific. Keep them but add KDoc documenting expected values.

### 2.4 Error Categorization

**Add `ErrorKind` to `EnrichmentResult.Error`:**
```kotlin
data class Error(
    val type: EnrichmentType,
    val provider: String,
    val message: String,
    val cause: Throwable? = null,
    val errorKind: ErrorKind = ErrorKind.UNKNOWN,
) : EnrichmentResult()

enum class ErrorKind { NETWORK, AUTH, PARSE, NOT_SUPPORTED, UNKNOWN }
```

### 2.5 `HttpClient` Response Metadata

**Problem:** `fetchJson()` returns `JSONObject?` -- null hides whether it was 404, 429, 500, or network error. Root cause of the MusicBrainz empty-results bug.

**Add typed response methods alongside existing ones:**
```kotlin
interface HttpClient {
    suspend fun fetchJson(url: String): JSONObject?  // keep for compatibility
    suspend fun fetchJsonResult(url: String): HttpResult<JSONObject>  // NEW
}

sealed class HttpResult<T> {
    data class Ok<T>(val data: T, val statusCode: Int = 200) : HttpResult<T>()
    data class ClientError<T>(val statusCode: Int, val body: String?) : HttpResult<T>()
    data class ServerError<T>(val statusCode: Int, val body: String?) : HttpResult<T>()
    data class RateLimited<T>(val retryAfterMs: Long?) : HttpResult<T>()
    data class NetworkError<T>(val cause: Throwable) : HttpResult<T>()
}
```

Migrate providers to use `fetchJsonResult()` over time. Providers can now distinguish 404 vs 429 vs 500 precisely.

### Verification
- Serialization round-trip tests for changed data classes (cache compat)
- `EnrichmentIdentifiers` get/withExtra tests
- E2E tests pass with cleaned-up types
- Compile and test: `./gradlew :musicmeta-core:test`

---

## Phase 3: ROADMAP Phase 1 -- New Types & Capabilities (~2-3 weeks)

**Goal:** Add 6 new enrichment types + artwork sizes enhancement using the new abstraction layers.

### 3.1 New `EnrichmentType` Values

Add to enum:
```kotlin
BAND_MEMBERS(DAYS_30, RELATIONSHIPS),
ARTIST_DISCOGRAPHY(DAYS_30, METADATA),
ALBUM_TRACKS(DAYS_365, METADATA),
SIMILAR_TRACKS(DAYS_30, RELATIONSHIPS),
ARTIST_BANNER(DAYS_90, ARTWORK),
ARTIST_LINKS(DAYS_90, METADATA),
```

### 3.2 New `EnrichmentData` Subclasses

```kotlin
data class BandMembers(val members: List<BandMember>) : EnrichmentData()
data class Discography(val albums: List<DiscographyAlbum>) : EnrichmentData()
data class Tracklist(val tracks: List<TrackInfo>) : EnrichmentData()
data class SimilarTracks(val tracks: List<SimilarTrack>) : EnrichmentData()
data class ArtistLinks(val links: List<ExternalLink>) : EnrichmentData()
```

Supporting data classes (all use `EnrichmentIdentifiers` instead of bare `musicBrainzId`):
```kotlin
data class BandMember(name, role?, activePeriod?, identifiers)
data class DiscographyAlbum(title, year?, type?, thumbnailUrl?, identifiers)
data class TrackInfo(title, position, durationMs?, identifiers)
data class SimilarTrack(title, artist, matchScore, identifiers)
data class ExternalLink(type, url, label?)  // type = "official", "spotify", etc.
```

### 3.3 Artwork Sizes Enhancement

**Enhance `EnrichmentData.Artwork`:**
```kotlin
data class Artwork(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailUrl: String? = null,
    val sizes: List<ArtworkSize>? = null,  // NEW
) : EnrichmentData()

data class ArtworkSize(url: String, width: Int?, height: Int?, label: String?)
```

**Mapper updates per provider:**
| Provider | Sizes Available |
|----------|----------------|
| Cover Art Archive | 250px, 500px, 1200px, full -- from JSON metadata endpoint |
| Deezer | 56px (small), 250px (medium), 500px (big), 1000px (xl) |
| iTunes | Any size via `{n}x{n}bb` URL template (generate 250, 500, 1000, 3000) |
| Fanart.tv | Full resolution + community variants |

### 3.4 Provider Implementations

#### BAND_MEMBERS
| Provider | Implementation | Effort |
|----------|---------------|--------|
| MusicBrainz | Add `inc=artist-rels` to `lookupArtist()`. Parse `relations[]` where `type == "member of band"`. New mapper method. | Medium |
| Discogs | New endpoint: `GET /artists/{id}`. Parse `members[]` array. Need Discogs artist ID (store in `identifiers.extra["discogsArtistId"]` during search). | Medium |

#### ARTIST_DISCOGRAPHY
| Provider | Implementation | Effort |
|----------|---------------|--------|
| MusicBrainz | **New API endpoint:** browse `GET /release-group?artist={mbid}&type=album|ep|single&limit=100`. Parse release groups. | Medium |
| Deezer | **New API endpoint:** `GET /artist/{id}/albums`. Need Deezer artist ID from search. | Low |

#### ALBUM_TRACKS
| Provider | Implementation | Effort |
|----------|---------------|--------|
| MusicBrainz | **Data already returned** in release lookup `media[]` array -- just not parsed. Add `parseMedia()` to parser. | Low |
| Deezer | **New API endpoint:** `GET /album/{id}/tracks`. | Low |

#### SIMILAR_TRACKS
| Provider | Implementation | Effort |
|----------|---------------|--------|
| Last.fm | **New API method:** `track.getSimilar`. Add to `LastFmApi`. | Low |
| Deezer | **New API endpoint:** `GET /artist/{id}/related` (artist-level, supplements track similarity). | Low |

#### ARTIST_BANNER
| Provider | Implementation | Effort |
|----------|---------------|--------|
| Fanart.tv | **Already parsed** -- `FanartTvArtistImages.banners` (mapped as `musicbanner`). Just add capability + case in `enrichFromImages()`. | Very low |

#### ARTIST_LINKS
| Provider | Implementation | Effort |
|----------|---------------|--------|
| MusicBrainz | **Already fetched** via `inc=url-rels`. Currently only extracts Wikidata/Wikipedia URLs. Expand parser to extract ALL URL relation types (official site, Spotify, YouTube, Instagram, etc.) and map to `ExternalLink`. | Low |

### 3.5 More from Wikidata

Expand `WikidataApi` to fetch multiple properties in one call:

| Property | Code | Maps To |
|----------|------|---------|
| Image | P18 | `ARTIST_PHOTO` (existing) |
| Birth date | P569 | `Metadata.beginDate` |
| Death date | P570 | `Metadata.endDate` |
| Country of origin | P495 | `Metadata.country` |
| Occupation | P106 | `Metadata.artistType` enrichment |
| Commons category | P373 | Future: more images |

Also combine the Wikidata call from WikipediaProvider (sitelinks resolution) with WikidataProvider's call to reduce API calls.

### Verification
- Serialization round-trip tests for all new data classes
- Unit tests per provider mapper for new types (with sample JSON fixtures)
- Provider capability tests
- E2E tests for each new type
- `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

---

## Phase 4: ROADMAP Phase 2 -- Deepen Existing Types (~2 weeks)

### 4.1 Back Cover & Booklet Art

**New types:** `ALBUM_ART_BACK`, `ALBUM_BOOKLET` (add to `EnrichmentType` enum)

**Cover Art Archive:** Use JSON metadata endpoint `GET /release/{mbid}/` which returns full `images[]` array with `types` field. Currently only requesting `front-{size}`. Parse all image types.

### 4.2 Album Metadata from More Sources

Wire unused data from existing API responses:

| Provider | Available Data (Currently Ignored) |
|----------|-----------------------------------|
| Deezer | `fans`, `nb_tracks`, `record_type`, `release_date`, `explicit_lyrics`, `rating`, `genres`, `label`, `duration`, `contributors`, `UPC` |
| iTunes | `trackCount`, `primaryGenreName`, `collectionPrice` |
| Discogs | `community.rating`, `community.have/want`, `catno`, `genres`, `styles` |

Add these to `Metadata` where appropriate. Add new fields to `Metadata` as needed (e.g., `trackCount: Int?`, `explicit: Boolean?`).

### 4.3 Track-Level Popularity (Fix)

Phase 0 removed Last.fm's `TRACK_POPULARITY` capability. Now implement it properly:

| Provider | Implementation |
|----------|---------------|
| Last.fm | New method: `track.getInfo` returning playcount/listeners per track |
| ListenBrainz | New endpoint: `POST /1/popularity/recording` (batch lookup by MBIDs) |

### 4.4 Wikipedia Page Media

**New endpoint in `WikipediaApi`:** `/page/media-list/{title}` returns all images on the article (band photos, album covers, concert shots). Supplements thin `ARTIST_PHOTO` coverage.

### 4.5 ListenBrainz Batch Endpoints

Wire currently untapped high-value endpoints:

| Endpoint | Use |
|----------|-----|
| `POST /1/popularity/recording` | Batch recording popularity by MBIDs |
| `POST /1/popularity/artist` | Batch artist popularity |
| `GET /1/popularity/top-release-groups-for-artist/{mbid}` | Top albums by listen count |
| `GET /1/stats/sitewide/artists` | Global top artists |

### 4.6 Confidence Scoring Standardization

Add a `ConfidenceCalculator` utility that providers use (convention, not enforcement):

```kotlin
object ConfidenceCalculator {
    fun idBasedLookup() = 1.0f              // Deterministic MBID/ID lookup
    fun authoritative() = 0.95f             // Wikipedia, exact LRCLIB match
    fun searchScore(score: Int, max: Int = 100) = (score.toFloat() / max).coerceIn(0f, 1f)
    fun fuzzyMatch(hasArtistMatch: Boolean) = if (hasArtistMatch) 0.8f else 0.6f
}
```

Update all providers to use the calculator for consistency.

### Verification
- Unit tests for each new endpoint/parser
- E2E tests for deepened types
- Confidence calculator tests
- Full regression: `./gradlew :musicmeta-core:test`

---

## Critical Files Reference

| File | Phases Touched | What Changes |
|------|---------------|-------------|
| `EnrichmentProvider.kt` | 1 | `IdentifierRequirement`, `isIdentityProvider`, `identityTypes`, `resolveIdentity()` |
| `EnrichmentData.kt` | 1, 2, 3 | Remove `IdentifierResolution`, clean `SimilarArtist`/`PopularTrack`, add 5 new subclasses, artwork sizes |
| `EnrichmentType.kt` | 2, 3, 4 | Constructor params (TTL, category), 8+ new values |
| `EnrichmentConfig.kt` | 1, 2 | `ApiKeyConfig`, `ttlOverrides` |
| `DefaultEnrichmentEngine.kt` | 1, 2 | Refactor `resolveIdentity()`, remove `ttlFor()`, remove `IDENTITY_TYPES` |
| `ProviderChain.kt` | 1 | Typed `hasRequiredIdentifiers()` |
| `ProviderRegistry.kt` | 1 | `isIdentityProvider` selection |
| `EnrichmentRequest.kt` | 2 | `EnrichmentIdentifiers.extra` |
| `EnrichmentResult.kt` | 2 | `ErrorKind` on `Error` |
| `HttpClient.kt` | 2 | `fetchJsonResult()`, `HttpResult` sealed class |
| `EnrichmentEngine.kt` | 1 | Builder: `apiKeys()`, `withDefaultProviders()` |
| All 11 `*Provider.kt` | 1, 3, 4 | Mapper extraction, new capabilities, new endpoints |
| All 11 `*Api.kt` | 3, 4 | New API endpoints |

## Sequencing

```
Phase 0 (bugs) --- no deps, do first
    |
Phase 1 (abstraction) --- foundation for everything
    |
Phase 2 (API cleanup) --- depends on Phase 1
    |
Phase 3 (new types) --- depends on Phase 1 mappers + Phase 2 extensibility
    |
Phase 4 (deepening) --- depends on Phase 3 types existing
```

Within phases, sub-items are largely independent and can be parallelized.

## Versioning

| Phase | Version | Notes |
|-------|---------|-------|
| 0 | 0.1.1 | Bug fixes only |
| 1+2 | 0.2.0 | Breaking: `IdentifierResolution` removed, `SimilarArtist.musicBrainzId` removed, `ProviderCapability.requiresIdentifier` replaced |
| 3 | 0.3.0 | Additive: 6 new types, artwork sizes |
| 4 | 0.4.0 | Additive: 2 new types, deepened providers, batch endpoints |
