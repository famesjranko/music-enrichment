# Last.fm Provider

> Community-driven music data. Primary source for similar artists and genre tags. Good artist popularity stats from decades of scrobble data.

## API Overview

| | |
|---|---|
| **Base URL** | `https://ws.audioscrobbler.com/2.0/` |
| **Auth** | API key (query param `api_key`) |
| **Rate Limit** | 5 requests/second (we use 200ms) |
| **Format** | JSON (`&format=json`) |
| **Reference Docs** | https://www.last.fm/api |
| **Get API Key** | https://www.last.fm/api/account/create |
| **API Key Required** | Yes |

> **Note:** HTTPS is supported and recommended. API keys are sent as query parameters, so using HTTP exposes them in cleartext. Our code currently hardcodes `http://` — this should be updated.

## Getting an API Key

1. Create a Last.fm account at https://www.last.fm/join
2. Create an API application at https://www.last.fm/api/account/create
3. You get an **API Key** (public) and a **Shared Secret** (only needed for authenticated methods — we don't use those)
4. Pass the API key as `lastfm.apikey` system property or `LASTFM_API_KEY` env var

## Endpoints We Use

### artist.getinfo
```
GET /?method=artist.getinfo&artist={name}&api_key={key}&format=json
```

Response:
```json
{
  "artist": {
    "name": "Radiohead",
    "bio": { "summary": "...", "content": "..." },
    "tags": { "tag": [{ "name": "alternative", "url": "..." }] },
    "stats": { "listeners": "5123456", "playcount": "312000000" },
    "similar": { "artist": [...] },
    "image": [{ "#text": "url", "size": "small|medium|large|extralarge|mega" }]
  }
}
```

We extract: name, bio.summary, tags, stats.listeners, stats.playcount.

### artist.getsimilar
```
GET /?method=artist.getsimilar&artist={name}&api_key={key}&format=json&limit=20
```

Response:
```json
{
  "similarartists": {
    "artist": [
      { "name": "Thom Yorke", "match": "0.87", "mbid": "..." }
    ]
  }
}
```

We extract: name, match (float 0-1), mbid.

## What We Extract

| Field | Source | Used For |
|-------|--------|----------|
| Bio summary | `artist.bio.summary` | ARTIST_BIO (priority 50, fallback to Wikipedia) |
| Genre tags | `artist.tags.tag[].name` | GENRE (priority 100) |
| Similar artists | `similarartists.artist[]` | SIMILAR_ARTISTS |
| Listener count | `artist.stats.listeners` | ARTIST_POPULARITY |
| Play count | `artist.stats.playcount` | ARTIST_POPULARITY |

## What We DON'T Extract (Available Data)

### From Current Responses

| Field | Where | Useful For |
|-------|-------|------------|
| `artist.bio.content` | artist.getinfo | Full bio text (not just summary) |
| `artist.image[]` | artist.getinfo | Artist images at multiple sizes. **Warning:** Last.fm stopped serving most artist images ~2020. The `image` array still appears in responses but URLs are typically empty or broken. |
| `artist.similar.artist[]` | artist.getinfo | Similar artists (redundant with getsimilar, but saves a call) |
| `artist.url` | artist.getinfo | Last.fm profile URL |

### From Endpoints Not Yet Called

| Endpoint | Data | Useful For |
|----------|------|------------|
| `artist.gettoptracks` | Top tracks by playcount | TRACK_POPULARITY, ARTIST_POPULARITY |
| `artist.gettopalbums` | Top albums by playcount | Album rankings |
| `album.getinfo` | Album bio, playcount, listeners, tags, wiki, images | Album metadata & popularity |
| `track.getinfo` | Track playcount, listeners, tags, wiki | TRACK_POPULARITY |
| `track.getsimilar` | Similar tracks with match scores | SIMILAR_TRACKS |
| `tag.gettopartists` | Top artists for a genre tag | Genre exploration |
| `tag.gettopalbums` | Top albums for a genre tag | Genre exploration |
| `chart.getTopArtists` | Sitewide top artists chart | Trending/popular artists |
| `chart.getTopTracks` | Sitewide top tracks chart | Trending/popular tracks |

## Gotchas & Edge Cases

- **Bio contains HTML**: `bio.summary` often includes `<a href="...">` links. Consumers should strip HTML or render it.
- **Bio may include a "Read more" link**: The summary often ends with `<a href="https://www.last.fm/music/...">Read more on Last.fm</a>`. You may want to strip this.
- **Artist-only provider**: Currently only handles `ForArtist` requests. Album and track requests return `NotFound`. This is the biggest gap — Last.fm has rich album/track data.
- **`match` score for similar artists**: Float 0.0–1.0 where 1.0 = most similar. We pass this through as `matchScore`.
- **Tags are user-contributed**: Quality varies. "rock" and "alternative" are reliable; long-tail tags like "albums I listened to in 2019" appear but sort low. We sort by vote count.
- **Empty API key = provider disabled**: `isAvailable` checks if the key is non-blank. Gracefully degrades.
- **Rate limit**: 5 req/s is generous but shared across all Last.fm calls in a session. Our 200ms limiter stays within bounds.
- **HTTP not HTTPS**: The API base URL uses `http://` — it works with HTTPS too but the official docs reference HTTP. HTTPS is strongly recommended since API keys travel as query params.
- **Stats are cumulative**: Listener/playcount numbers represent all-time Last.fm scrobbles. Active scrobblers skew toward certain demographics.
- **`mbid` parameter**: Most methods accept an `mbid` parameter for MusicBrainz ID-based lookups instead of name matching. When available, prefer `mbid` over `artist`/`album`/`track` name params for precise results.
- **`autocorrect` parameter**: Most methods accept `autocorrect=1` to enable fuzzy name matching (e.g., "Radiohd" corrects to "Radiohead"). Default is 0 (off).
- **Error response format**: Errors return `{"error": <code>, "message": "..."}`. Key error codes: 6 = not found, 10 = invalid API key, 29 = rate limit exceeded.
- **TRACK_POPULARITY mismatch**: The provider declares TRACK_POPULARITY capability but `enrichPopularity()` calls `artist.getinfo` which returns artist-level stats, not track-level data. This should use `track.getinfo` instead.

## Internal Architecture

```
LastFmProvider
├── LastFmApi       — HTTP calls + parsing (inline, no separate parser)
└── LastFmModels    — DTOs: LastFmArtistInfo, LastFmSimilarArtist
```

Constructor params:
- `apiKey: String` (or `apiKeyProvider: () -> String` for lazy loading)
- `httpClient: HttpClient`
- `rateLimiter: RateLimiter` — 200ms recommended
