# Wikidata Provider

> Structured knowledge base. Currently used only for artist photos (P18 property), but contains a wealth of structured data about artists, albums, and recordings.

## API Overview

| | |
|---|---|
| **Base URL** | `https://www.wikidata.org/w/api.php` |
| **Auth** | None |
| **Rate Limit** | ~5 requests/second for unauthenticated clients. Exceeding returns `maxlag` errors or HTTP 429. We use 100ms. |
| **Format** | JSON (`&format=json`) |
| **Reference Docs** | https://www.wikidata.org/wiki/Wikidata:Data_access |
| **API Docs** | https://www.wikidata.org/w/api.php?action=help |
| **Property Directory** | https://www.wikidata.org/wiki/Wikidata:List_of_properties |
| **API Key Required** | No |

## How Wikidata Works

Wikidata stores structured data as **entities** (items starting with Q) that have **properties** (starting with P). Each property has one or more **claims** (statements with values).

Example: Radiohead (Q44191) has:
- P18 (image) = "Radiohead at Austin City Limits 2016.jpg"
- P569 (birth date) — N/A for groups
- P571 (inception) = 1985
- P27 (country of citizenship) — N/A for groups
- P495 (country of origin) = United Kingdom (Q145)
- P136 (genre) = alternative rock, art rock, electronic music
- P264 (record label) = Parlophone, XL Recordings, Capitol Records
- P527 (has parts) = Thom Yorke (Q44857), Jonny Greenwood (Q192668), etc.

## Endpoints We Use

### Get Claims (single property)
```
GET /w/api.php?action=wbgetclaims&entity={wikidataId}&property=P18&format=json
```

Response:
```json
{
  "claims": {
    "P18": [
      {
        "mainsnak": {
          "snaktype": "value",
          "property": "P18",
          "datavalue": {
            "value": "Radiohead at Austin City Limits 2016.jpg",
            "type": "string"
          }
        },
        "rank": "normal"
      }
    ]
  }
}
```

### Image URL Construction

Wikidata stores filenames, not URLs. We construct Wikimedia Commons URLs:

```
https://commons.wikimedia.org/wiki/Special:FilePath/{filename}?width={size}
```

Spaces → underscores. Note: `Special:FilePath?width=` now auto-renders SVG and TIFF to PNG thumbnails. Appending `.png` to the filename (as our code does) is no longer strictly required but is harmless.

## What We Extract

| Field | Source | Notes |
|-------|--------|-------|
| Artist photo URL | P18 claim → Commons URL | First claim only, at configured size (default 1200px) |

## What We DON'T Extract (Available Data)

Wikidata is a **treasure trove** of structured data. We only touch P18.

### Music-Relevant Properties

| Property | Code | Data Type | Useful For |
|----------|------|-----------|------------|
| **Image** | P18 | filename | **Currently extracted** |
| Genre | P136 | Item (Q-ID) | GENRE — structured, not free-text tags |
| Record label | P264 | Item (Q-ID) | LABEL |
| Country of origin | P495 | Item (Q-ID) | COUNTRY |
| Country of citizenship | P27 | Item (Q-ID) | Artist nationality |
| Inception date | P571 | time | Band formation date |
| Dissolution date | P576 | time | Band breakup date |
| Birth date | P569 | time | Artist birth |
| Death date | P570 | time | Artist death |
| Has parts / Members | P527 | Item (Q-ID) | BAND_MEMBERS (links to member Q-IDs) |
| Part of | P361 | Item (Q-ID) | What groups an artist belongs to |
| Occupation | P106 | Item (Q-ID) | "Singer", "Guitarist", "Composer" |
| Gender | P21 | Item (Q-ID) | |
| Instrument | P1303 | Item (Q-ID) | What instrument they play |
| Official website | P856 | URL | ARTIST_LINKS |
| Discography | P358 | Item (Q-ID) | Links to discography article |
| Awards received | P166 | Item (Q-ID) | Grammy, Brit Awards, etc. |
| Nominated for | P1411 | Item (Q-ID) | Award nominations |
| Commons category | P373 | string | More images on Wikimedia Commons |
| Social media | P2013 (Facebook), P2002 (Twitter), P2003 (Instagram) | string | ARTIST_LINKS |
| Spotify artist ID | P1902 | string | Streaming cross-reference |
| MusicBrainz artist ID | P434 | string | Reverse lookup |
| AllMusic artist ID | P1728 | string | Cross-reference |
| Discogs artist ID | P1953 | string | Cross-reference to Discogs |
| Performer | P175 | Item (Q-ID) | Track/album performer |
| Producer | P162 | Item (Q-ID) | Producer credits |
| Lyrics by | P676 | Item (Q-ID) | Lyricist credits |
| Composer | P86 | Item (Q-ID) | Composer credits |
| Publication date | P577 | time | Release / publication date |
| MusicBrainz release group ID | P4404 | string | Cross-reference to MusicBrainz release groups |
| MusicBrainz work ID | P4407 | string | Cross-reference to MusicBrainz works |
| MusicBrainz recording ID | P8052 | string | Cross-reference to MusicBrainz recordings |

### Full Entity Endpoint (not called)

```
GET /w/api.php?action=wbgetentities&ids={wikidataId}&format=json
```

Returns ALL properties, labels, descriptions, and sitelinks in one call. Much more efficient than requesting individual properties if we need multiple.

```
GET /w/api.php?action=wbgetentities&ids={wikidataId}&props=claims|descriptions|labels|sitelinks&format=json
```

The `sitelinks` property is how Wikipedia provider resolves Wikipedia article titles — could be done in one Wikidata call instead of two separate ones.

### Wikidata REST API

Wikidata has a newer REST API at `https://www.wikidata.org/w/rest.php/wikibase/v1/` with endpoints like `GET /entities/items/{id}/statements?property=P18`. It returns cleaner JSON and is recommended for new integrations.

### SPARQL Endpoint

The SPARQL endpoint at `https://query.wikidata.org/sparql` allows complex batch queries — e.g., fetching P18, P136, P264, P527 for an artist in one request. More efficient than individual `wbgetclaims` calls.

## User-Agent Requirement

Wikidata/Wikimedia APIs expect a descriptive User-Agent header. Requests without one may be throttled or blocked. Our `DefaultHttpClient` handles this.

## `maxlag` Parameter

The `maxlag` parameter is recommended for all requests to handle server load gracefully. When the server is under heavy load, requests with `maxlag` will receive a retry-later response instead of stale data.

## Gotchas & Edge Cases

- **Requires Wikidata ID**: No text search from this provider. Depends on MusicBrainz identity resolution to provide the `wikidataId`.
- **Properties return Q-IDs, not labels**: P136 (genre) returns `Q11399` not "alternative rock". To get the human-readable name, you'd need a separate entity lookup or use the labels prop. For common values, a local lookup table is more efficient.
- **Image filename encoding**: Filenames may contain spaces, parentheses, diacritics. Must URL-encode after replacing spaces with underscores.
- **SVG/TIFF handling**: If the image is SVG or TIFF, Commons can render it as PNG — append `.png` to the URL. Our code handles this, though `Special:FilePath?width=` now auto-renders these formats.
- **Multiple claims per property**: An artist might have multiple P18 images. Claims have `rank`: "preferred", "normal", "deprecated". We take `[0]` — should respect rank ordering.
- **Claim rank selection**: Our code takes `[0]` from the claims array, but the API does not guarantee ordering by rank. Should filter for `rank: preferred` first, then `normal`, skipping `deprecated`.
- **Qualifiers on claims**: Claims can have qualifiers (e.g., P527 "has parts" might have P580 "start time" and P582 "end time" qualifiers for when a member joined/left). Important for band member histories.
- **Not all entities are equal**: Coverage varies wildly. Major artists have 50+ properties; obscure ones might have only a name.
- **WikiCommons Special:FilePath**: This URL does a redirect to the actual image. It supports `?width=` for on-the-fly resizing (up to original dimensions).
- **Redundant HTTP request**: The Wikipedia provider also calls `wbgetentities` for sitelinks. A single call with `props=claims|sitelinks` could serve both providers, eliminating a redundant HTTP request.

## Internal Architecture

```
WikidataProvider
└── WikidataApi       — single property fetch + Commons URL construction
```

No Models file — parsing is inline in the API class.

Constructor params:
- `httpClient: HttpClient`
- `rateLimiter: RateLimiter` — 100ms is fine
- `imageSize: Int = 1200` — pixel width for image URLs
