# MusicBrainz Provider

> The identity backbone of the enrichment engine. Resolves MBIDs, Wikidata IDs, and Wikipedia titles that downstream providers use for precise lookups.

## API Overview

| | |
|---|---|
| **Base URL** | `https://musicbrainz.org/ws/2` |
| **Auth** | None — but a descriptive `User-Agent` is **required** |
| **Rate Limit** | 1 request/second average (sliding window; we use 1100ms). Returns HTTP 503 when exceeded. |
| **Format** | JSON (`?fmt=json`). Also accepts `Accept: application/json` header. |
| **Query Syntax** | Lucene (special chars must be escaped) |
| **Reference Docs** | https://musicbrainz.org/doc/MusicBrainz_API |
| **Search Docs** | https://musicbrainz.org/doc/MusicBrainz_API/Search |
| **Rate Limit Docs** | https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting |
| **API Key Required** | No (OAuth2 available for write operations) |

## User-Agent Requirement

MusicBrainz will block requests without a descriptive User-Agent. Format:

```
AppName/Version (contact-url-or-email)
```

Example: `MusicMetaShowcase/1.0 (https://github.com/famesjranko/musicmeta)`

This is set via `DefaultHttpClient(userAgent)` and applies to all providers, but MusicBrainz is the one that enforces it.

## API Request Types

MusicBrainz has three request types:

1. **Search** — Lucene full-text queries. Returns scored results but no relationships or sub-entity details. Supports `limit` and `offset` for pagination.
2. **Lookup** — Fetch a single entity by MBID with `inc` parameters to include related data. Always returns score 100.
3. **Browse** — Retrieve all entities linked to another entity (e.g., all release-groups by an artist). Supports `inc` parameters, `limit`, and `offset`. Unlike search, results are unscored but complete.

Browse is essential for features like artist discography and is more efficient than search for linked-entity traversal.

## Endpoints We Use

### Search: Release
```
GET /ws/2/release?query={lucene}&fmt=json&limit={n}&offset={n}
```
Lucene query: `release:"OK Computer" AND artist:"Radiohead"`

Returns: `releases[]` — each with id, title, artist-credit, date, country, barcode, tags, label-info, release-group (id, primary-type), cover-art-archive.front, disambiguation, score.

### Search: Artist
```
GET /ws/2/artist?query={lucene}&fmt=json&limit={n}&offset={n}
```
Lucene query: `artist:"Radiohead"`

Returns: `artists[]` — each with id, name, type, country, life-span, tags, disambiguation, score. **No relations in search** — need lookup for those.

Note: Our provider re-ranks artist candidates via `pickBestArtist()` — prioritizing exact name match with tags over raw score. This means the highest-scoring MusicBrainz result may not be selected.

### Search: Recording
```
GET /ws/2/recording?query={lucene}&fmt=json&limit={n}&offset={n}
```
Lucene query: `recording:"Bohemian Rhapsody" AND artist:"Queen"`

Returns: `recordings[]` — each with id, title, tags, score, length, first-release-date, artist-credit, releases[].

Note: ISRCs are **not** returned in search results — only available via lookup with `inc=isrcs`.

### Lookup: Release
```
GET /ws/2/release/{mbid}?fmt=json&inc=artist-credits+labels+release-groups+tags
```
Full release detail. Score is always 100 for direct lookups.

### Lookup: Artist
```
GET /ws/2/artist/{mbid}?fmt=json&inc=tags+url-rels
```
Full artist detail with URL relations (wikidata, wikipedia, etc).

### Lookup: Recording (not yet used)
```
GET /ws/2/recording/{mbid}?fmt=json&inc=artist-credits+isrcs+tags+releases
```
Full recording detail with ISRCs (not available in search), linked releases, and credits.

## What We Extract

| Field | Source | Notes |
|-------|--------|-------|
| MBID | `id` | Primary identifier for all downstream providers |
| Release Group ID | `release-group.id` | Used by Cover Art Archive as fallback |
| Title | `title` | |
| Artist Credit | `artist-credit[].artist.name + joinphrase` | Handles "feat." and "&" cases |
| Date | `date` | Format varies: "2003", "2003-06", "2003-06-09" |
| Country | `country` | ISO 3166-1 alpha-2 |
| Barcode | `barcode` | UPC/EAN |
| Tags/Genres | `tags[].name` (sorted by `count` desc) | Falls back to `release-group.tags` |
| Label | `label-info[0].label.name` | First label only |
| Release Type | `release-group.primary-type` | "Album", "Single", "EP", etc. |
| Wikidata ID | `relations[type=wikidata].url.resource` → extract Q-ID | Lookup only (not in search) |
| Wikipedia Title | `relations[type=wikipedia].url.resource` → extract title | Lookup only |
| Has Front Cover | `cover-art-archive.front` | Boolean — avoids 404s on CAA |
| Artist Type | `type` | "Person", "Group", "Orchestra", "Choir", etc. |
| Life Span | `life-span.begin`, `life-span.end` | Band formation/dissolution or birth/death |
| Disambiguation | `disambiguation` | Distinguishes same-name entities |

## What We DON'T Extract (Available Data)

### From Current Responses (just ignored)

| Field | Where | Useful For |
|-------|-------|------------|
| `media[]` (tracklist) | Release lookup | ALBUM_TRACKS — track titles, positions, durations, ISRCs |
| `media[].format` | Release lookup | Vinyl, CD, Digital, etc. |
| `relations[]` (non-wiki) | Artist lookup | All URL relations: official site, bandcamp, spotify, youtube, twitter, etc. |
| `status` | Release | "Official", "Promotion", "Bootleg" |
| `packaging` | Release | "Jewel Case", "Digipak", etc. |
| `text-representation.language` | Release | Album language |
| `artist.gender` | Artist lookup | Male/Female/Non-binary/Other |
| `artist.area` | Artist lookup | More specific than country |
| `recording.length` | Recording search | Track duration in ms |
| `recording.first-release-date` | Recording | When track first appeared |
| `cover-art-archive.count` | Release | Total number of images available |
| `cover-art-archive.back` | Release | Boolean — has back cover art |

### From Endpoints Not Yet Called

| Endpoint | Type | Data | Useful For |
|----------|------|------|------------|
| `GET /ws/2/release-group?artist={mbid}&type=album&limit=100&offset=0` | Browse | All release groups by artist | ARTIST_DISCOGRAPHY |
| `GET /ws/2/recording/{mbid}?inc=artist-credits+isrcs+tags+releases` | Lookup | Full recording detail with ISRCs | Track metadata |
| `GET /ws/2/artist/{mbid}?inc=artist-rels+tags+url-rels` | Lookup | Band member relationships | BAND_MEMBERS |
| `GET /ws/2/release/{mbid}?inc=recordings+artist-credits+recording-level-rels` | Lookup | Tracklist with per-track credits | ALBUM_TRACKS, CREDITS |
| `GET /ws/2/recording/{mbid}?inc=artist-rels+work-rels` | Lookup | Composer, lyricist, performer | CREDITS |
| `GET /ws/2/work/{id}?inc=artist-rels` | Lookup | Songwriting credits | CREDITS |
| `GET /ws/2/release-group/{mbid}?inc=releases+tags+genres` | Lookup | All releases in a group + genre data | RELEASE_EDITIONS |

### `genres` vs `tags` inc parameter

MusicBrainz has a newer `genres` inc parameter that returns curated genre data (as opposed to free-form user-submitted `tags`). Using `inc=genres` returns a `genres[]` array that is more reliable for genre classification. Consider using `inc=genres` alongside or instead of `inc=tags`.

### Relationship Types Available

When `inc=artist-rels` is added, the `relations[]` array contains:
- `member of band` — who is/was in the group (with time periods)
- `collaboration` — joint projects
- `is person` — real name behind stage name
- `supporting musician` — live/session musicians
- `vocal`, `instrument`, `performer` — recording credits

When `inc=url-rels` is added (already used):
- `wikidata`, `wikipedia` — **currently extracted**
- `official homepage`, `bandcamp`, `soundcloud`, `youtube`, `social network`, `streaming`, `discogs`, `allmusic`, `setlist.fm`, `songkick` — **not extracted**

## Gotchas & Edge Cases

- **Rate limiting is strict**: 1 req/sec average (sliding window). Exceeding it returns HTTP 503. Our `RateLimiter(1100)` adds buffer. Authenticated requests (OAuth2) may receive preferential treatment.
- **Lucene special chars**: Characters `+ - & | ! ( ) { } [ ] ^ " ~ * ? : \` must be escaped. `MusicBrainzApi.escapeLucene()` handles this (also escapes `/` which is harmless). Watch for artist names like "AC/DC", "Guns N' Roses", "!!!".
- **Search vs Lookup**: Search results include metadata (tags, labels) but **not** URL relations. Lookups include relations but cost an extra request. The provider only does lookups when the requested type needs relations (ARTIST_PHOTO, ARTIST_BIO, etc.).
- **Score interpretation**: Search scores are 0–100. We map directly to confidence (score/100). `minMatchScore` (default 80) filters poor matches.
- **Artist ranking**: For artist search, `pickBestArtist()` re-ranks candidates by exact name match + tag presence before applying the score threshold. The highest-scoring result may not be selected.
- **Artist credit join phrases**: "The Beatles" is simple, but "Eminem feat. Rihanna" has a joinphrase " feat. ". The parser concatenates `artist.name + joinphrase` for each credit.
- **Tags on release-groups, not releases**: Genre tags are primarily on release-groups in MusicBrainz. The parser falls back: `release.tags` → `release-group.tags`. Note: the `inc=tags` on release lookup only returns release-level tags; release-group tags require `inc=release-group-level-rels` or a separate lookup.
- **Empty results ≠ rate limited**: Empty search results with HTTP 200 mean "not found." Rate limiting returns HTTP 503. Our code currently treats empty results as `RateLimited` — this is a known bug that misclassifies legitimate "not found" cases.
- **Date format varies**: Can be "2003", "2003-06", or "2003-06-09" — consumers should handle all three.
- **Search pagination**: Search supports `offset` parameter alongside `limit` for paginating through large result sets. We currently only fetch the first page.

## Internal Architecture

```
MusicBrainzProvider
├── MusicBrainzApi          — HTTP calls + rate limiting + Lucene escaping
├── MusicBrainzParser       — JSON → DTOs (extractors for tags, labels, relations, etc.)
└── MusicBrainzModels       — DTOs: MusicBrainzRelease, MusicBrainzArtist, MusicBrainzRecording
```

Constructor params:
- `httpClient: HttpClient` — shared HTTP client
- `rateLimiter: RateLimiter` — should be 1100ms for MusicBrainz
- `minMatchScore: Int = 80` — minimum search score to accept
- `thumbnailSize: Int = 250` — CAA thumbnail size for search candidates
