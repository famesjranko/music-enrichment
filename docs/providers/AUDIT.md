# Provider Doc Audit — Findings Summary

> Cross-referenced our docs against real third-party API documentation, code, and live endpoints.
> Audit date: 2026-03-21

---

## MusicBrainz

### Inaccuracies
- **Empty results treated as RateLimited is wrong.** MusicBrainz returns HTTP 503 for rate limiting, not 200 with empty results. Empty results genuinely mean "not found." Our code (`MusicBrainzProvider.kt` lines 129, 170, 200) and doc both have this wrong — legitimate "not found" cases are misclassified as rate-limited.
- **ISRCs not returned in recording search.** Doc claims recording search returns ISRCs, but the MusicBrainz search API does not include `isrcs` in search results — only in lookups via `inc=isrcs`. The parser tries to extract them but they'll always be empty from search.
- **`inc=tags` on release lookup doesn't include release-group tags.** The `extractReleaseTags()` fallback to `release-group.tags` only works if the release-group object happens to include tags — which it doesn't with just `inc=tags` at the release level. Need `inc=release-group-level-rels` or separate lookup.
- **`artistname` search field** works but `artist` is the canonical/preferred field name in official docs.

### Missing from Doc
- **Browse endpoints** — a major omission. Third request type alongside search and lookup: `GET /ws/2/release-group?artist={mbid}&type=album&limit=100&offset=0`. Supports `inc` params unlike search. Essential for discography.
- **`genres` inc parameter** — newer addition returning curated genre data (more reliable than user-submitted `tags`). `inc=genres` returns `genres[]` array.
- **`offset` parameter** for search pagination.
- **Recording lookup endpoint** — `GET /ws/2/recording/{mbid}?inc=artist-credits+isrcs+tags+releases` not documented.
- **`pickBestArtist()` ranking logic** undocumented — artist search re-ranks by exact name match + tag presence, not just score.

---

## Cover Art Archive

### Inaccuracies
- **Doc claims "No rate limiting rules"** — verified correct per official docs: "There are currently no rate limiting rules in place."
- **Our doc's image types list is incomplete.** Only mentions "Front, Back, Booklet" etc. informally.

### Missing from Doc (confirmed via live API docs)
- **`/release/{mbid}/back` endpoint** — explicitly documented, works same as front.
- **`/release/{mbid}/{id}` endpoint** — fetch specific image by archive.org internal ID.
- **JSON metadata endpoint** (`GET /release/{mbid}/`) returns full `images[]` array with: `types`, `front`, `back`, `comment`, `approved`, `edit`, `id`, and `thumbnails` object with keys `"250"`, `"500"`, `"1200"` plus deprecated `"small"` and `"large"`.
- **HTTP 503 for rate limiting** — the API does return 503 even though no formal limits are documented.
- **`"small"` and `"large"` thumbnail keys are deprecated** — use `"250"` and `"500"` instead.

---

## Last.fm

### Inaccuracies
- **`album.getsimilar` does not exist.** Listed in our doc as an available endpoint — it has never existed in the Last.fm API. Remove it.
- **Artist images are largely defunct.** Since ~2020, Last.fm stopped serving most artist images. The `image` array appears in responses but URLs are typically empty/broken. Doc lists them as available data.
- **Base URL should be HTTPS.** Code hardcodes `http://ws.audioscrobbler.com/2.0/`. HTTPS is fully supported and preferred — API keys transmitted in cleartext over HTTP is a security risk.
- **`TRACK_POPULARITY` returns artist-level data.** Code declares this capability but `enrichPopularity()` calls `artist.getinfo` which returns artist-level listeners/playcount, not track-level.

### Missing from Doc
- **`mbid` parameter** — most methods accept a MusicBrainz ID for precise lookups instead of fuzzy name matching.
- **`artist.getTopTags`** — dedicated endpoint returning more tags with vote counts (better than extracting from `artist.getinfo`).
- **`autocorrect` parameter** — fuzzy matching/spelling correction on artist names.
- **Error response format** — `{"error": <code>, "message": "..."}` with error code 6 = not found, 10 = invalid key, 29 = rate limited.
- **`chart.*` methods** — `chart.getTopArtists`, `chart.getTopTracks` for trending data.

---

## Fanart.tv

### Inaccuracies
- **"Free tier gives access to all images" is wrong.** Free tier gets images older than 7 days. VIP members get immediate access to newly uploaded images.

### Missing from Doc
- **`client_key` parameter** — optional second auth param for VIP tier access. Separate from `api_key`.
- **Album-specific endpoint** — `GET /v3/music/albums/{album_mbid}?api_key={key}` returns album images directly without fetching entire artist response. More efficient for `ForAlbum` requests.
- **Label endpoint** — `GET /v3/music/labels/{label_mbid}?api_key={key}` with `musiclabel` image type (400x270 label logos).
- **HTTP error codes** — 401 (invalid key), 429 (rate limited) not documented.

---

## Deezer

### Inaccuracies
- **Rate limit numbers unconfirmed.** Doc says "50 requests / 5 seconds" but Deezer's official docs don't publish specific numbers publicly. The actual enforcement may differ.

### Missing from Doc
- **Deezer docs are behind a login wall** — developers.deezer.com requires login to view full API docs. This itself should be noted.
- **OAuth 2.0 available** for user-specific data (playlists, recommendations). Not needed for public search.
- **API appears stable and publicly accessible** as of March 2026 — no sunset announcements found.

### Confirmed
- Base URL `https://api.deezer.com` is correct.
- No auth needed for search endpoints.
- Search, artist, album, track endpoints all appear active.

---

## Discogs

### Inaccuracies
- **Token as query param is less preferred.** Discogs recommends `Authorization: Discogs token=...` header instead. Query param works but leaks tokens in logs.
- **Rate limiter inconsistency** — doc says "100ms" but code comment says "1000ms interval." At 60 req/min, even spacing = 1000ms.
- **Image CDN User-Agent claim** — doc says images "may return 403" without User-Agent. Actually, `cover_image` and `thumb` from search results are publicly accessible. Only images from detailed endpoints need auth.

### Missing from Doc
- **`Authorization` header method** (recommended over query param).
- **User-Agent requirement** — Discogs API requires descriptive User-Agent; requests without may be throttled/rejected.
- **Rate limit response headers** — `X-Discogs-Ratelimit`, `X-Discogs-Ratelimit-Used`, `X-Discogs-Ratelimit-Remaining`.
- **`master_id` and `master_url`** in search results — links release to master release.
- **`catno`** (catalog number) in search results.
- **`community` field** on detailed releases — `have`, `want` counts + `rating` with `average` and `count`.
- **`year: "0"` edge case** — unknown year returns `"0"` not blank; code's `takeIf { it.isNotBlank() }` doesn't filter this.

---

## ListenBrainz

### Inaccuracies (confirmed via live API docs)
- **Several "not yet called" endpoints in our doc are wrong or don't exist:**
  - `GET /1/stats/artist/{mbid}/listeners` — actual path is `GET /1/stats/artist/{artist_mbid}/listeners`
  - `GET /1/popularity/top-recordings` — doesn't exist as a global endpoint
  - `GET /1/popularity/top-artists` — doesn't exist
  - `GET /1/popularity/top-artists-for-genre` — doesn't exist
- **Response has more fields than documented.** Each recording object also includes: `recording_name` (not `track_name` in all contexts), `release` info (mbid, name, color), `caa_id`, `caa_release_mbid`, `length`, `total_user_count`.

### Missing from Doc (confirmed via live API docs)
- **`GET /1/popularity/top-release-groups-for-artist/{mbid}`** — top albums by listen count. High value, not mentioned at all.
- **`POST /1/popularity/recording`** — batch lookup of recording popularity by MBIDs. Returns `total_listen_count` and `total_user_count` per recording.
- **`POST /1/popularity/artist`** — batch lookup of artist popularity by MBIDs.
- **`POST /1/popularity/release`** — batch lookup of release popularity.
- **`POST /1/popularity/release-group`** — batch lookup of release group popularity.
- **Massive statistics API** — user, artist, and sitewide stats with time range filtering (`this_week`, `month`, `quarter`, `year`, `all_time`). Includes:
  - `/1/stats/sitewide/artists` — global top artists
  - `/1/stats/sitewide/recordings` — global top recordings
  - `/1/stats/artist/{mbid}/listeners` — artist listener stats
  - `/1/stats/release-group/{mbid}/listeners` — album listener stats
- **Art API** — album art generation endpoints.
- **Explore/recommendation endpoints** for discovery.
- **`total_user_count` field** in response — distinct listener count (we only extract `total_listen_count`).

---

## LRCLIB

### Inaccuracies
- **"Duration is in seconds as an integer" is wrong.** API returns `duration` as a float (seconds with decimals). Our model correctly uses `Double?`, but the request conversion `(it / 1000).toInt()` loses precision.

### Missing from Doc
- **`q` parameter on search** — `/api/search` also supports `?q={freetext}` for free-text search.
- **`POST /api/publish` endpoint** — for submitting lyrics (requires solving a cryptographic challenge).
- **`POST /api/request-challenge` endpoint** — returns proof-of-work challenge for spam prevention.
- **`Lrclib-Client` User-Agent header** — LRCLIB requests a descriptive User-Agent.
- **HTTP 429 for rate limiting** — returns 429 when limits exceeded (limits not published).

---

## Wikidata

### Inaccuracies
- **"Rate limit: None documented" is wrong.** Wikimedia enforces ~5 req/s for unauthenticated clients. Exceeding returns `maxlag` errors or HTTP 429.
- **P17 (country) in Radiohead example** is wrong property — should be P495 (country of origin) for bands.
- **SVG/TIFF `.png` appending** is not strictly necessary — `Special:FilePath?width=` auto-renders non-raster formats.
- **Claim rank handling** — code takes `[0]` blindly but API doesn't guarantee ordering by rank. Should filter for `"rank": "preferred"` first.

### Missing from Doc
- **Wikidata REST API** — newer, cleaner API at `https://www.wikidata.org/w/rest.php/wikibase/v1/`. Recommended for new integrations.
- **SPARQL endpoint** — `https://query.wikidata.org/sparql` for complex/batch queries. Single query can fetch multiple properties at once.
- **`maxlag` parameter** — recommended for all requests to handle server load.
- **Missing music properties** — P175 (performer), P162 (producer), P676 (lyrics by), P86 (composer), P577 (publication date), P4404/P4407/P8052 (MusicBrainz cross-refs for albums/works/recordings).
- **Can combine with Wikipedia provider's call** — both call `wbgetentities`; a single call with `props=claims|sitelinks` could serve both.

---

## Wikipedia

### Inaccuracies
- **`/page/mobile-sections/{title}` is deprecated/removed.** Was part of legacy Mobile Content Service, deprecated in 2023. Should not be listed as a future endpoint.
- **Rate limit understated.** Doc says "Not strictly enforced." Wikimedia actively throttles clients without proper User-Agent and can block. ~200 req/s ceiling.
- **`extract` field described as "2-3 paragraphs"** — actually just the lead section summary (first paragraph or few sentences). Length varies significantly.

### Missing from Doc
- **`extract_html` field** — same content as `extract` but with HTML formatting. Available in `/page/summary` response.
- **`type` field values** — `"standard"`, `"disambiguation"`, `"no-extract"`, `"mainpage"`. Checking `type` can detect disambiguation pages programmatically.
- **New Wikimedia API** at `api.wikimedia.org` — newer API gateway with OAuth2, cleaner design, explicit language routing. Recommended for new integrations.
- **Language support** — just change subdomain: `fr.wikipedia.org/api/rest_v1/...` for French, etc.
- **`revision` and `tid` fields** — useful for cache invalidation.

---

## iTunes

### Inaccuracies
- **`country` parameter is technically required** per Apple's docs (default `US`). Our doc says "defaults to US" which is functionally true but Apple's spec marks it required.
- **`artworkUrl60` also returned** — not just `artworkUrl100`. Both exist in responses; our doc/code only references the 100px version.
- **Default `limit` is 50, max is 200** — not documented by us.
- **`country` format mismatch** — request parameter uses 2-letter ISO (`US`, `GB`) but response field returns 3-letter (`USA`, `GBR`). Doc doesn't clarify this.

### Missing from Doc
- **`attribute` parameter** — restricts which field `term` matches against. Values: `albumTerm`, `artistTerm`, `songTerm`, `composerTerm`, `mixTerm`, `genreIndex`. Using `attribute=albumTerm` could significantly improve search precision.
- **UPC/barcode lookup** — `GET /lookup?upc={barcode}` for precise, ID-based album lookup. High value when we have barcode data from MusicBrainz/Discogs.
- **Apple Music API (MusicKit)** — richer alternative with editorial notes, ISRC codes, charts, recommendations. Requires Apple Developer account + JWT auth.
- **Enterprise Partner Feed (EPF)** — Apple's official recommendation for high-volume usage.
- **`explicit` parameter** — `explicit=Yes|No` to filter adult content.
- **Multiple ID lookups** — `GET /lookup?id=909253,284910350` (comma-separated).
- **`sort=recent`** on lookup endpoint for artist discography ordering.
- **Docs are in Documentation Archive** (dated 2017-09-19) — API works but docs may not reflect undocumented changes since then.

### Confirmed Accurate
- Base URL, search endpoint format, no auth, ~20 req/min rate limit all correct.
- Artwork URL `100x100bb` → `{n}x{n}bb` trick confirmed working and stable.
- `3000x3000bb` as practical max resolution confirmed.
- All documented response fields confirmed present.
- API is still active with no sunset announcements.
- No rate limit headers returned (confirmed).

---

## Top Priority Fixes Across All Providers

| # | Fix | Providers | Severity |
|---|-----|-----------|----------|
| 1 | Empty results = RateLimited is a bug | MusicBrainz | High (code + doc) |
| 2 | Remove `album.getsimilar` — doesn't exist | Last.fm | High (wrong info) |
| 3 | Switch Last.fm to HTTPS | Last.fm | High (security) |
| 4 | Fix rate limit claims | Wikidata, Wikipedia, MusicBrainz | Medium |
| 5 | Add ListenBrainz batch popularity endpoints | ListenBrainz | Medium (missing capability) |
| 6 | Add ListenBrainz `top-release-groups-for-artist` | ListenBrainz | Medium (high value) |
| 7 | Document Fanart.tv album endpoint | Fanart.tv | Medium (efficiency) |
| 8 | Document Wikidata REST API + SPARQL | Wikidata | Medium (future-proofing) |
| 9 | Fix LRCLIB duration to float | LRCLIB | Low (precision loss) |
| 10 | Add `genres` inc param | MusicBrainz | Low (better genre data) |
| 11 | Switch Discogs to Authorization header | Discogs | Low (best practice) |
| 12 | Note Wikipedia `/page/mobile-sections` deprecation | Wikipedia | Low |
| 13 | Add `attribute` param for targeted search | iTunes | Low (better precision) |
| 14 | Add UPC/barcode lookup endpoint | iTunes | Low (ID-based lookup) |
