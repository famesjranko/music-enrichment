# musicmeta PRD: v0.5.0 — New Capabilities & Tech Debt Cleanup

## Context

musicmeta is at v0.4.0 with 25 enrichment types, 11 providers, a clean abstraction layer (typed identifiers, mapper pattern, formalized identity resolution, standardized confidence scoring), and 328 passing tests. Provider utilization averages ~50%.

The remaining ROADMAP items are the "Phase 3: New Capabilities" features — credits, release editions, artist timeline, genre deep dive — plus v0.4.0 tech debt (HttpResult adoption, ErrorKind wiring, minor provider gaps). These features complete the "app-ready" story: a music app using musicmeta would have everything it needs for artist pages, album pages, and now-playing screens.

This PRD covers: tech debt cleanup, credits & personnel, release editions, artist timeline, and genre enhancement.

---

## Phase 0: Tech Debt Cleanup

Quick wins from v0.4.0 deferred items. No new types — just wiring existing infrastructure.

### 0.1 ListenBrainz ARTIST_DISCOGRAPHY Wiring
- **Files:** `provider/listenbrainz/ListenBrainzProvider.kt`
- **What:** API (`getTopReleaseGroupsForArtist`) and mapper (`toDiscography`) already exist. Just add `ProviderCapability(ARTIST_DISCOGRAPHY, priority = 50, identifierRequirement = MUSICBRAINZ_ID)` and route the type in `enrich()`.
- **Effort:** Very low — ~5 lines of code.

### 0.2 HttpResult Migration
- **Files:** All 11 `*Api.kt` files, `FakeHttpClient.kt`
- **What:** Migrate 27 `fetchJson()`/`fetchJsonArray()` call sites to use `fetchJsonResult()` which returns `HttpResult<T>` with distinct subtypes for Ok/ClientError/ServerError/RateLimited/NetworkError. Currently all error types are masked as null returns.
- **Pattern:** Replace `httpClient.fetchJson(url) ?: return null` with:
  ```kotlin
  when (val result = httpClient.fetchJsonResult(url)) {
      is HttpResult.Ok -> result.data
      is HttpResult.RateLimited -> throw RateLimitedException(result.retryAfterMs)
      is HttpResult.NetworkError -> throw result.cause
      else -> return null  // 404, 500 — genuine not-found
  }
  ```
- **Why now:** Enables ErrorKind categorization (0.3) and prevents silent error masking.
- **Effort:** Low — mechanical, 27 call sites following same pattern.

### 0.3 ErrorKind Adoption
- **Files:** All 11 `*Provider.kt` files
- **Depends on:** Phase 0.2 (HttpResult migration)
- **What:** 8 `EnrichmentResult.Error()` constructions across providers all default to `ErrorKind.UNKNOWN`. With HttpResult in place, categorize errors:
  - `HttpResult.NetworkError` → `ErrorKind.NETWORK`
  - `HttpResult.RateLimited` → `ErrorKind.RATE_LIMIT`
  - `HttpResult.ClientError(401/403)` → `ErrorKind.AUTH`
  - `JSONException` / parse failures → `ErrorKind.PARSE`
- **Effort:** Low — once HttpResult is in place, each provider catch block maps exception type to ErrorKind.

### 0.4 Discogs: Store Release ID and Master ID
- **Files:** `provider/discogs/DiscogsModels.kt`, `provider/discogs/DiscogsApi.kt`
- **What:** Search results already return `id` (release ID) and `master_id` but both are discarded during parsing. Store them on `DiscogsRelease` model. Prerequisite for Credits (Phase 2) and Release Editions (Phase 3).
- **Effort:** Very low — add 2 fields to DTO, parse 2 extra JSON fields.

### Verification
- All existing tests pass
- ListenBrainz ARTIST_DISCOGRAPHY returns results in E2E
- HttpResult call sites return proper error types (unit test per provider Api)
- ErrorKind values are set correctly (unit test per provider)
- `./gradlew :musicmeta-core:test`

---

## Phase 1: Credits & Personnel

**Goal:** Consumers can enrich track-level and album-level credits (producers, performers, composers, engineers) from MusicBrainz and Discogs.

### 1.1 New Types and Data Model

**Add to `EnrichmentType.kt`:**
```kotlin
CREDITS(30L * 24 * 60 * 60 * 1000),  // 30 days TTL
```

**Add to `EnrichmentData.kt`:**
```kotlin
@Serializable
data class Credits(val credits: List<Credit>) : EnrichmentData()

@Serializable
data class Credit(
    val name: String,
    val role: String,               // "lead vocals", "producer", "guitar", "composer"
    val roleCategory: String? = null, // "performance", "production", "songwriting"
    val identifiers: EnrichmentIdentifiers = EnrichmentIdentifiers(),
)
```

Role categories group the many specific roles into 3 buckets:
- **performance** — vocal, instrument, performer, conductor, DJ, chorus
- **production** — producer, engineer, mixer, mastering, phonogram producer, recording
- **songwriting** — composer, lyricist, arranger, librettist

### 1.2 MusicBrainz: Recording Credits

**Source:** `GET /ws/2/recording/{mbid}?inc=artist-rels+work-rels` (per docs/providers/musicbrainz.md line 137)

This returns the `relations[]` array on the recording with credit-type relationships. The pattern is identical to the existing `parseBandMembers()` in `MusicBrainzParser.kt` — filter by type, extract artist name/id/attributes.

**Relationship types to parse:**
| MusicBrainz `type` | Role | Category |
|---------------------|------|----------|
| `vocal` | attributes[0] (e.g., "lead vocals") | performance |
| `instrument` | attributes[0] (e.g., "guitar", "drums") | performance |
| `performer` | "performer" | performance |
| `producer` | "producer" | production |
| `engineer` | "engineer" | production |
| `mixer` | "mixer" | production |
| `mastering` | "mastering" | production |
| `recording` | "recording engineer" | production |
| `composer` | "composer" (from work-rels) | songwriting |
| `lyricist` | "lyricist" (from work-rels) | songwriting |
| `arranger` | "arranger" | songwriting |

**Implementation:**
- **MusicBrainzApi:** Add `lookupRecording(mbid: String): MusicBrainzRecordingDetail?` calling the endpoint above.
- **MusicBrainzModels:** Add `MusicBrainzRecordingDetail(id, title, credits: List<MusicBrainzCredit>)` and `MusicBrainzCredit(name, id, roles, roleCategory)`.
- **MusicBrainzParser:** Add `parseRecordingCredits(json: JSONObject): List<MusicBrainzCredit>` following the `parseBandMembers()` pattern — iterate `relations[]`, filter by credit types, extract artist + attributes.
- **MusicBrainzMapper:** Add `toCredits(credits: List<MusicBrainzCredit>): EnrichmentData.Credits`.
- **MusicBrainzProvider:** Add `ProviderCapability(CREDITS, priority = 100, identifierRequirement = MUSICBRAINZ_ID)`. Add `enrichCredits()` for `ForTrack` requests. Requires recording MBID — needs identity resolution first.

**Note:** For album-level credits (all tracks), the engine would need to call `lookupRelease` with `inc=recording-level-rels` (line 136 in musicbrainz.md). This is a single API call that returns credits for all tracks on the release. Consider supporting `ForAlbum` requests by aggregating per-track credits from this endpoint.

### 1.3 Discogs: Release Credits

**Source:** `GET /releases/{id}` (per docs/providers/discogs.md line 92)

The response includes `extraartists[]` at release level and `tracklist[].extraartists[]` at track level, each with `name`, `role`, and `resource_url`.

**Prerequisite:** Phase 0.4 (store release ID from search results).

**Implementation:**
- **DiscogsApi:** Add `getReleaseDetails(releaseId: Long): DiscogsReleaseDetails?` calling `GET /releases/{id}`.
- **DiscogsModels:** Add `DiscogsReleaseDetails(tracklist: List<DiscogsTrack>, extraartists: List<DiscogsCredit>, companies: List<DiscogsCompany>)`, `DiscogsTrack(position, title, extraartists: List<DiscogsCredit>)`, `DiscogsCredit(name, role, id)`.
- **DiscogsMapper:** Add `toCredits(credits: List<DiscogsCredit>): EnrichmentData.Credits`. Map Discogs role strings to roleCategory using a lookup (Discogs uses free-text roles like "Producer", "Vocals", "Written-By").
- **DiscogsProvider:** Add `ProviderCapability(CREDITS, priority = 50)`. Requires the release ID from search (stored via Phase 0.4). For `ForTrack` requests, filter `tracklist[].extraartists` by matching track title/position. For `ForAlbum`, use release-level `extraartists[]`.

### Verification
- Unit tests with canned JSON for MusicBrainz recording credits (vocals, instruments, producer, composer)
- Unit tests with canned JSON for Discogs release credits
- Serialization round-trip test for `Credits` and `Credit`
- E2E: `enrich(ForTrack("Karma Police", "Radiohead"), setOf(CREDITS))` returns credits with at least one performer
- `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

---

## Phase 2: Release Editions

**Goal:** Consumers can list all editions/pressings of an album (original, deluxe, remaster, vinyl, CD, regional variants) from MusicBrainz and Discogs.

### 2.1 New Types and Data Model

**Add to `EnrichmentType.kt`:**
```kotlin
RELEASE_EDITIONS(365L * 24 * 60 * 60 * 1000),  // 1 year TTL — editions rarely change
```

**Add to `EnrichmentData.kt`:**
```kotlin
@Serializable
data class ReleaseEditions(val editions: List<ReleaseEdition>) : EnrichmentData()

@Serializable
data class ReleaseEdition(
    val title: String,
    val format: String? = null,       // "CD", "Vinyl", "Digital", "Cassette"
    val country: String? = null,      // "UK", "US", "Japan"
    val year: Int? = null,
    val label: String? = null,
    val catalogNumber: String? = null, // "NODATA 02", "7243 8 55229 2 8"
    val barcode: String? = null,
    val identifiers: EnrichmentIdentifiers = EnrichmentIdentifiers(),
)
```

### 2.2 MusicBrainz: Release Group Editions

**Source:** `GET /ws/2/release-group/{mbid}?inc=releases+tags+genres` (per docs/providers/musicbrainz.md line 139)

Returns all releases in a release-group (the abstract "album" concept). Each release is a specific pressing with country, date, format, barcode, label.

**Implementation:**
- **MusicBrainzApi:** Add `lookupReleaseGroup(releaseGroupMbid: String): MusicBrainzReleaseGroupDetail?`.
- **MusicBrainzModels:** Add `MusicBrainzReleaseGroupDetail(id, title, releases: List<MusicBrainzEdition>)` and `MusicBrainzEdition(id, title, date, country, barcode, status, format, labelInfo)`.
- **MusicBrainzParser:** Add `parseReleaseGroupDetail(json: JSONObject)` — parse `releases[]` array with format from `media[].format`, label from `label-info[].label.name`, catalog number from `label-info[].catalog-number`.
- **MusicBrainzMapper:** Add `toReleaseEditions(editions: List<MusicBrainzEdition>): EnrichmentData.ReleaseEditions`.
- **MusicBrainzProvider:** Add `ProviderCapability(RELEASE_EDITIONS, priority = 100, identifierRequirement = MUSICBRAINZ_RELEASE_GROUP_ID)`. Requires `musicBrainzReleaseGroupId` from identity resolution.

### 2.3 Discogs: Master Versions

**Source:** `GET /masters/{master_id}/versions` (per docs/providers/discogs.md line 94)

Returns all versions/pressings with format, label, country, year.

**Prerequisite:** Phase 0.4 (store master_id from search results).

**Implementation:**
- **DiscogsApi:** Add `getMasterVersions(masterId: Long): List<DiscogsMasterVersion>`.
- **DiscogsModels:** Add `DiscogsMasterVersion(id, title, format, label, country, year, catno)`.
- **DiscogsMapper:** Add `toReleaseEditions(versions: List<DiscogsMasterVersion>): EnrichmentData.ReleaseEditions`.
- **DiscogsProvider:** Add `ProviderCapability(RELEASE_EDITIONS, priority = 50)`. Requires `masterId` stored during search.

### Verification
- Unit tests with canned JSON for MusicBrainz release-group editions
- Unit tests with canned JSON for Discogs master versions
- Serialization round-trip test for `ReleaseEditions` and `ReleaseEdition`
- E2E: `enrich(ForAlbum("OK Computer", "Radiohead"), setOf(RELEASE_EDITIONS))` returns at least 3 editions (UK CD, US CD, vinyl)
- `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

---

## Phase 3: Artist Timeline

**Goal:** Consumers can request a structured timeline for an artist combining life-span, discography, and membership changes into chronological events.

### 3.1 New Type and Data Model

**Add to `EnrichmentType.kt`:**
```kotlin
ARTIST_TIMELINE(30L * 24 * 60 * 60 * 1000),  // 30 days TTL
```

**Add to `EnrichmentData.kt`:**
```kotlin
@Serializable
data class ArtistTimeline(val events: List<TimelineEvent>) : EnrichmentData()

@Serializable
data class TimelineEvent(
    val date: String,               // "1985", "1985-04", "1985-04-22"
    val type: String,               // "formed", "first_album", "member_joined", "member_left",
                                    // "album_release", "hiatus_start", "hiatus_end", "disbanded"
    val description: String,        // "Band formed in Abingdon, Oxfordshire"
    val relatedEntity: String? = null, // album title, member name, etc.
    val identifiers: EnrichmentIdentifiers = EnrichmentIdentifiers(),
)
```

### 3.2 Implementation: Composite Enrichment

Artist Timeline is unique — it's a **composite type** that synthesizes data from other enrichment types rather than calling a single API. The data sources are:

1. **MusicBrainz artist lookup** (already fetched during identity resolution):
   - `life-span.begin` → "formed" / "born" event
   - `life-span.end` → "disbanded" / "died" event
   - `life-span.ended` → whether ended

2. **Wikidata properties** (already fetched in Phase 4):
   - P569 (birth date), P570 (death date) — supplements MusicBrainz dates

3. **ARTIST_DISCOGRAPHY results** (already available as enrichment type):
   - Each album's year → "album_release" events
   - First album → "first_album" event
   - Latest album → "latest_album" event

4. **BAND_MEMBERS results** (already available):
   - Member begin/end dates → "member_joined" / "member_left" events

**Two implementation approaches:**

**A. Engine-level composite (recommended):** Add a `CompositeEnrichmentResolver` in the engine that requests ARTIST_DISCOGRAPHY + BAND_MEMBERS + GENRE internally, then synthesizes a timeline. This avoids creating a new provider and reuses existing data.

**B. Provider-level composite:** Create a `TimelineProvider` that internally calls MusicBrainz APIs and assembles the timeline. More isolated but duplicates API calls.

**Recommendation:** Approach A. The engine already has the data from identity resolution (life-span) and can request sub-types. The timeline type should be flagged as `composite = true` in the engine so it triggers sub-enrichments first.

### 3.3 Implementation Details

- **New engine concept:** `CompositeType` — an enrichment type that depends on other types being resolved first. The engine resolves dependencies, then calls a synthesis function.
- **TimelineSynthesizer.kt** (new file in `engine/`): Takes `EnrichmentRequest` + resolved sub-results → produces `ArtistTimeline` by:
  1. Extracting dates from identity resolution metadata (begin/end dates)
  2. Extracting album release years from Discography
  3. Extracting member join/leave dates from BandMembers
  4. Sorting all events chronologically
  5. Deduplicating overlapping events (MusicBrainz + Wikidata dates)

### Verification
- Unit test: synthesizer produces timeline with known inputs
- Serialization round-trip test for `ArtistTimeline` and `TimelineEvent`
- E2E: `enrich(ForArtist("Radiohead"), setOf(ARTIST_TIMELINE))` returns timeline with at least "formed" and multiple "album_release" events
- `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

---

## Phase 4: Genre Enhancement

**Goal:** Genre results carry per-tag confidence scores and merge data from multiple providers with deduplication.

### 4.1 Enhanced Data Model

**Modify `EnrichmentData.Metadata`** — change genre field:
```kotlin
// Before:
val genres: List<String>? = null,

// After:
val genres: List<String>? = null,           // keep for backward compat
val genreTags: List<GenreTag>? = null,      // new: enriched genre data
```

**Add to `EnrichmentData.kt`:**
```kotlin
@Serializable
data class GenreTag(
    val name: String,           // "alternative rock"
    val confidence: Float,      // 0.0-1.0
    val sources: List<String>,  // ["musicbrainz", "lastfm", "discogs"]
)
```

### 4.2 Genre Merging Strategy

A new `GenreMerger` utility (in `engine/`) handles combining genre data from multiple providers:

1. **Normalize** — lowercase, trim, map common aliases ("alt rock" → "alternative rock", "hip hop" → "hip-hop")
2. **Deduplicate** — group by normalized name
3. **Score** — Each provider vote adds confidence:
   - MusicBrainz tag with count > 5: +0.4
   - Last.fm top tag: +0.3
   - Discogs genre: +0.3 (curated taxonomy)
   - Discogs style: +0.2 (more specific)
   - iTunes primaryGenre: +0.2
   - Multiple sources agreeing: cap at 1.0
4. **Rank** — Sort by combined confidence desc
5. **Populate both fields** — `genres` gets the top-N tag names (backward compatible), `genreTags` gets the full scored list

### 4.3 Provider Changes

No new API calls needed — all genre data is already fetched:
- **MusicBrainz:** `artist.tags` from search (already parsed)
- **Last.fm:** `getArtistTopTags()` (already called)
- **Discogs:** `genres` + `styles` from search (already parsed in v0.4.0)
- **iTunes:** `primaryGenreName` from search (already parsed)

The change is in how the engine merges results. Currently, the provider chain short-circuits on first `Success` — MusicBrainz genres win and Last.fm/Discogs are never tried. For genre, the engine should collect results from ALL providers and merge.

**Two approaches:**
**A. Multi-provider merge in engine (recommended):** Add a `mergeableTypes` concept — types where all provider results are collected and merged rather than short-circuiting. GENRE would be the first such type.
**B. Dedicated GenreProvider:** A meta-provider that internally queries others. More isolated but adds complexity.

### 4.4 Implementation

- **GenreMerger.kt** (new file in `engine/`): Normalize, deduplicate, score, rank.
- **Mapper updates:** Each provider's mapper adds `genreTags` to `Metadata` with per-provider confidence:
  - `MusicBrainzMapper.toGenre()` → set confidence from tag count
  - `LastFmMapper.toGenre()` → set confidence from position in tag list
  - `DiscogsMapper.toAlbumMetadata()` → set confidence 0.3 for genres, 0.2 for styles
- **ProviderChain modification:** For `mergeableTypes`, collect all `Success` results instead of short-circuiting. Pass to `GenreMerger.merge()`.
- **Backward compatibility:** `genres: List<String>?` continues to be populated with top tag names.

### Verification
- Unit tests for GenreMerger: normalization, deduplication, scoring, ranking
- Unit test: "alternative rock" from MusicBrainz + "Alternative Rock" from Last.fm → single tag with high confidence
- Serialization round-trip test for `GenreTag`
- E2E: `enrich(ForArtist("Radiohead"), setOf(GENRE))` returns `genreTags` with confidence scores and multiple sources
- `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

---

## Critical Files Reference

| File | Phases Touched | What Changes |
|------|---------------|-------------|
| `EnrichmentType.kt` | 1, 2, 3 | Add CREDITS, RELEASE_EDITIONS, ARTIST_TIMELINE |
| `EnrichmentData.kt` | 1, 2, 3, 4 | Add Credits, Credit, ReleaseEditions, ReleaseEdition, ArtistTimeline, TimelineEvent, GenreTag |
| `MusicBrainzApi.kt` | 1, 2 | Add lookupRecording(), lookupReleaseGroup() |
| `MusicBrainzModels.kt` | 1, 2 | Add MusicBrainzCredit, MusicBrainzRecordingDetail, MusicBrainzEdition, MusicBrainzReleaseGroupDetail |
| `MusicBrainzParser.kt` | 1, 2 | Add parseRecordingCredits(), parseReleaseGroupDetail() |
| `MusicBrainzMapper.kt` | 1, 2 | Add toCredits(), toReleaseEditions() |
| `MusicBrainzProvider.kt` | 1, 2 | Add CREDITS + RELEASE_EDITIONS capabilities |
| `DiscogsApi.kt` | 0, 1, 2 | Store release/master IDs, add getReleaseDetails(), getMasterVersions() |
| `DiscogsModels.kt` | 0, 1, 2 | Add id/masterId fields, DiscogsReleaseDetails, DiscogsCredit, DiscogsMasterVersion |
| `DiscogsMapper.kt` | 1, 2 | Add toCredits(), toReleaseEditions() |
| `DiscogsProvider.kt` | 1, 2 | Add CREDITS + RELEASE_EDITIONS capabilities |
| All 11 `*Api.kt` | 0 | HttpResult migration |
| All 11 `*Provider.kt` | 0 | ErrorKind adoption |
| `ListenBrainzProvider.kt` | 0 | Wire ARTIST_DISCOGRAPHY |
| `DefaultEnrichmentEngine.kt` | 3, 4 | CompositeType support, mergeableTypes support |
| `ProviderChain.kt` | 4 | Multi-result collection for mergeable types |
| `engine/TimelineSynthesizer.kt` | 3 | NEW — composite timeline assembly |
| `engine/GenreMerger.kt` | 4 | NEW — multi-provider genre merge |
| `provider/lastfm/LastFmApi.kt` | 5 | Add getAlbumInfo(), getArtistTopTracks() |
| `provider/lastfm/LastFmProvider.kt` | 5 | Add ALBUM_METADATA capability |
| `provider/itunes/ITunesApi.kt` | 5 | Add lookupAlbumTracks(), lookupArtistAlbums(); store IDs |
| `provider/itunes/ITunesProvider.kt` | 5 | Add ALBUM_TRACKS, ARTIST_DISCOGRAPHY capabilities |
| `provider/fanarttv/FanartTvApi.kt` | 5 | Add getAlbumImages() |
| `provider/fanarttv/FanartTvProvider.kt` | 5 | Modify ALBUM_ART path |
| `provider/listenbrainz/ListenBrainzApi.kt` | 5 | Add getSimilarArtists() |
| `provider/listenbrainz/ListenBrainzProvider.kt` | 5 | Add SIMILAR_ARTISTS capability |

## Sequencing

```
Phase 0 (tech debt) --- no deps, do first
    |
Phase 1 (credits) --- depends on Phase 0.4 (Discogs IDs)
    |
Phase 2 (release editions) --- depends on Phase 0.4 (Discogs IDs), can parallel with Phase 1
    |
Phase 3 (artist timeline) --- depends on existing DISCOGRAPHY + BAND_MEMBERS types
    |
Phase 4 (genre enhancement) --- independent, but benefits from Phase 0.2 (HttpResult)
```

Phases 1 and 2 can run in parallel after Phase 0. Phases 3 and 4 are independent of each other. Phase 5 is independent of Phases 3 and 4.

---

## Phase 5: Provider Coverage Expansion

**Goal:** Reach ~75% provider utilization by adding high-value, low-effort endpoints that deepen existing enrichment types with additional providers. No new enrichment types — purely more sources for existing types.

### 5.1 Last.fm: Album Info + Top Tracks

**New API methods in `LastFmApi`:**

**`album.getinfo`** — album biography, playcount, listeners, tags, wiki summary.
```
GET /?method=album.getinfo&artist={artist}&album={album}&api_key={key}&format=json
```
Returns:
- `album.wiki.summary` — album biography/description (HTML stripped)
- `album.listeners`, `album.playcount` — album-level popularity
- `album.tags.tag[]` — album-level genre tags
- `album.tracks.track[]` — tracklist with duration and rank

**`artist.gettoptracks`** — top tracks by scrobble count, better than ListenBrainz for non-MBID artists.
```
GET /?method=artist.gettoptracks&artist={artist}&api_key={key}&format=json&limit=10
```
Returns:
- `toptracks.track[]` — each with name, playcount, listeners, mbid, artist

**Implementation:**
- **LastFmApi:** Add `getAlbumInfo(album, artist)` and `getArtistTopTracks(artist, limit)`.
- **LastFmModels:** Add `LastFmAlbumInfo(name, artist, playcount, listeners, tags, wiki, tracks)` and `LastFmTopTrack(name, playcount, listeners, mbid)`.
- **LastFmMapper:** Add `toAlbumMetadata(info: LastFmAlbumInfo): EnrichmentData.Metadata` (maps playcount to popularity, tags to genres, wiki to a summary field). Add `toPopularity(tracks: List<LastFmTopTrack>): EnrichmentData.Popularity` for artist-level top tracks.
- **LastFmProvider:** Add capabilities:
  - `ProviderCapability(ALBUM_METADATA, priority = 40)` — between Deezer (50) and iTunes (30)
  - Enhance existing `ARTIST_POPULARITY` to include top tracks from `getArtistTopTracks`

**New enrichment coverage:**
- ALBUM_METADATA: 4th provider (Last.fm joins Deezer, Discogs, iTunes)
- ARTIST_POPULARITY: enriched with top track names and scrobble counts

### 5.2 iTunes: Album Tracks + Artist Discography Lookup

**New API methods in `ITunesApi`:**

**Album track lookup** — all tracks in an album by collection ID.
```
GET /lookup?id={collectionId}&entity=song&country=US
```
Returns: `results[]` where first item is the album, remaining items are tracks with trackName, trackNumber, trackTimeMillis, previewUrl, trackPrice, isStreamable.

**Artist discography lookup** — all albums by an artist by artist ID.
```
GET /lookup?id={artistId}&entity=album&country=US
```
Returns: `results[]` where first item is the artist, remaining items are albums with collectionName, releaseDate, trackCount, primaryGenreName, artworkUrl100.

**Prerequisite:** Store `collectionId` and `artistId` from search results in `EnrichmentIdentifiers.extra`. The current search response returns both but they're discarded.

**Implementation:**
- **ITunesApi:** Add `lookupAlbumTracks(collectionId: Long)` and `lookupArtistAlbums(artistId: Long)`.
- **ITunesModels:** Add `ITunesTrackResult(trackName, trackNumber, trackTimeMillis, previewUrl, discNumber)` and reuse existing `ITunesAlbumResult` for discography.
- **ITunesMapper:** Add `toTracklist(tracks: List<ITunesTrackResult>): EnrichmentData.Tracklist` and `toDiscography(albums: List<ITunesAlbumResult>): EnrichmentData.Discography`.
- **ITunesProvider:** Add capabilities:
  - `ProviderCapability(ALBUM_TRACKS, priority = 30)` — fallback behind MusicBrainz (100) and Deezer (50)
  - `ProviderCapability(ARTIST_DISCOGRAPHY, priority = 30)` — fallback behind MusicBrainz (100) and Deezer (50)
- **ITunesApi (search):** Store `collectionId` and `artistId` via `identifiers.withExtra("itunesCollectionId", ...)` during search.

**New enrichment coverage:**
- ALBUM_TRACKS: 3rd provider (iTunes joins MusicBrainz, Deezer)
- ARTIST_DISCOGRAPHY: 3rd provider (iTunes joins MusicBrainz, Deezer)

### 5.3 Fanart.tv: Album-Specific Art Endpoint

**New API method in `FanartTvApi`:**

```
GET /v3/music/albums/{mbid}?api_key={key}
```
Returns album-specific images: `albumcover[]` and `cdart[]` without needing the full artist response.

Currently, album art from Fanart.tv is extracted from the **artist** endpoint's nested `albums` object. This works but requires knowing the artist MBID and scanning through all albums. The album-specific endpoint is faster and more targeted.

**Implementation:**
- **FanartTvApi:** Add `getAlbumImages(releasGroupMbid: String): FanartTvAlbumImages?`.
- **FanartTvModels:** Add `FanartTvAlbumImages(albumcover: List<FanartTvImage>, cdart: List<FanartTvImage>)`.
- **FanartTvMapper:** Add `toAlbumArtwork(images: FanartTvAlbumImages): EnrichmentData.Artwork` with sizes from multiple images.
- **FanartTvProvider:** Modify ALBUM_ART enrichment to try album endpoint first (by release-group MBID), fall back to artist endpoint scan.

**New enrichment coverage:**
- ALBUM_ART: faster and more reliable Fanart.tv lookup for albums

### 5.4 ListenBrainz: Similar Artists

**New API method in `ListenBrainzApi`:**

```
GET /1/explore/lb-radio/artist/{artist_mbid}/similar?count=20
```
Returns artists similar to the given one based on listening patterns.

**Implementation:**
- **ListenBrainzApi:** Add `getSimilarArtists(artistMbid: String, count: Int = 20): List<ListenBrainzSimilarArtist>`.
- **ListenBrainzModels:** Add `ListenBrainzSimilarArtist(artistMbid, artistName, score)`.
- **ListenBrainzMapper:** Add `toSimilarArtists(artists: List<ListenBrainzSimilarArtist>): EnrichmentData.SimilarArtists`.
- **ListenBrainzProvider:** Add `ProviderCapability(SIMILAR_ARTISTS, priority = 50, identifierRequirement = MUSICBRAINZ_ID)` — fallback behind Last.fm (100).

**New enrichment coverage:**
- SIMILAR_ARTISTS: 2nd provider (ListenBrainz joins Last.fm). Works without API key, only needs MBID.

### 5.5 Discogs: Deeper Release Data (from PRD Phase 1 endpoint)

Phase 1 adds `GET /releases/{id}` for credits. The same response contains additional data currently ignored:

- `community.rating.average`, `community.rating.count` — album community rating
- `community.have`, `community.want` — collector interest signals
- `images[]` — multiple album images (primary, secondary) at full resolution
- `videos[]` — YouTube/Vimeo links for the album
- `notes` — release notes, sometimes contains album liner notes

**Implementation (additive to Phase 1):**
- **DiscogsMapper:** Extend `toAlbumMetadata()` to extract `communityRating` (already partially there), `have/want` counts, and notes.
- **DiscogsProvider:** The ALBUM_METADATA capability already exists at priority 40. Enrich the response with the deeper data from the release details call (which is already made for credits).
- No additional API calls — piggybacks on the credits endpoint.

**New enrichment coverage:**
- ALBUM_METADATA: significantly deeper Discogs data (community signals, full genre/style lists)

### Phase 5 Verification
- Unit tests per new API method with canned JSON responses
- E2E: Last.fm `album.getinfo` returns album metadata for "OK Computer"
- E2E: iTunes lookup returns tracklist for a known album
- E2E: ListenBrainz similar artists returns results for Radiohead MBID
- Provider capability counts increased (verify with showcase test)
- `./gradlew :musicmeta-core:test -Dinclude.e2e=true`

### Phase 5 Critical Files

| File | What Changes |
|------|-------------|
| `provider/lastfm/LastFmApi.kt` | Add getAlbumInfo(), getArtistTopTracks() |
| `provider/lastfm/LastFmModels.kt` | Add LastFmAlbumInfo, LastFmTopTrack |
| `provider/lastfm/LastFmMapper.kt` | Add toAlbumMetadata(), enhance toPopularity() |
| `provider/lastfm/LastFmProvider.kt` | Add ALBUM_METADATA capability |
| `provider/itunes/ITunesApi.kt` | Add lookupAlbumTracks(), lookupArtistAlbums(); store IDs from search |
| `provider/itunes/ITunesModels.kt` | Add ITunesTrackResult |
| `provider/itunes/ITunesMapper.kt` | Add toTracklist(), toDiscography() |
| `provider/itunes/ITunesProvider.kt` | Add ALBUM_TRACKS, ARTIST_DISCOGRAPHY capabilities |
| `provider/fanarttv/FanartTvApi.kt` | Add getAlbumImages() |
| `provider/fanarttv/FanartTvModels.kt` | Add FanartTvAlbumImages |
| `provider/fanarttv/FanartTvMapper.kt` | Add toAlbumArtwork() |
| `provider/fanarttv/FanartTvProvider.kt` | Modify ALBUM_ART enrichment path |
| `provider/listenbrainz/ListenBrainzApi.kt` | Add getSimilarArtists() |
| `provider/listenbrainz/ListenBrainzModels.kt` | Add ListenBrainzSimilarArtist |
| `provider/listenbrainz/ListenBrainzMapper.kt` | Add toSimilarArtists() |
| `provider/listenbrainz/ListenBrainzProvider.kt` | Add SIMILAR_ARTISTS capability |
| `provider/discogs/DiscogsMapper.kt` | Deepen toAlbumMetadata() with community data |

---

## Updated Sequencing

```
Phase 0 (tech debt) --- no deps, do first
    |
Phase 1 (credits) --- depends on Phase 0.4 (Discogs IDs)
    |
Phase 2 (release editions) --- depends on Phase 0.4 (Discogs IDs), can parallel with Phase 1
    |
Phase 3 (artist timeline) --- depends on existing DISCOGRAPHY + BAND_MEMBERS types
    |
Phase 4 (genre enhancement) --- independent, but benefits from Phase 0.2 (HttpResult)
    |
Phase 5 (provider coverage) --- independent, can parallel with Phases 3-4
```

Phases 1 and 2 can run in parallel after Phase 0. Phases 3, 4, and 5 are independent of each other and can all run in parallel.

## Updated Versioning

| Phase | Version | Notes |
|-------|---------|-------|
| 0 | 0.4.1 | Tech debt cleanup, no new public types |
| 1+2 | 0.5.0 | New types: CREDITS, RELEASE_EDITIONS |
| 3 | 0.5.0 | New type: ARTIST_TIMELINE + composite enrichment concept |
| 4 | 0.5.0 | Enhancement: Metadata.genreTags, ProviderChain mergeable behavior |
| 5 | 0.5.0 | Provider coverage expansion — no new types, just more providers for existing types |

## Updated Provider Utilization Projection

| Provider | Now (v0.4.0) | After Phase 0-4 | After Phase 5 | Total Documented |
|----------|:-:|:-:|:-:|:-:|
| **MusicBrainz** | 8/11 (73%) | 10/11 (91%) | 10/11 (91%) | 11 |
| **Last.fm** | 4/11 (36%) | 4/11 (36%) | 6/11 (55%) | 11 |
| **Fanart.tv** | 1/3 (33%) | 1/3 (33%) | 2/3 (67%) | 3 |
| **Deezer** | 5/7 (71%) | 5/7 (71%) | 5/7 (71%) | 7 |
| **Discogs** | 3/7 (43%) | 5/7 (71%) | 5/7 (71%) | 7 |
| **Cover Art Archive** | 3/5 (60%) | 3/5 (60%) | 3/5 (60%) | 5 |
| **ListenBrainz** | 6/14 (43%) | 6/14 (43%) | 7/14 (50%) | 14 |
| **iTunes** | 1/7 (14%) | 1/7 (14%) | 3/7 (43%) | 7 |
| **Wikidata** | 2/4 (50%) | 2/4 (50%) | 2/4 (50%) | 4 |
| **Wikipedia** | 3/5 (60%) | 3/5 (60%) | 3/5 (60%) | 5 |
| **LRCLIB** | 2/5 (40%) | 2/5 (40%) | 2/5 (40%) | 5 |
| **TOTAL** | **38/79 (48%)** | **42/79 (53%)** | **48/79 (61%)** | **79** |

Note: The 75% target in the earlier analysis counted only "high-value" endpoints. Against the full 79 documented endpoints (including niche ones like LRCLIB publish, Wikipedia deprecated mobile-sections, ListenBrainz CF recommendations), 61% is realistic. The remaining ~30 unused endpoints are mostly niche (charts, sitewide stats, write APIs, deprecated endpoints) with diminishing returns.
