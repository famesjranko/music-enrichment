---
phase: 06-tech-debt-cleanup
plan: 01
subsystem: http
tags: [kotlin, httpclient, httpresult, errorkind, providers, wikidata, wikipedia, fanarttv, itunes]

# Dependency graph
requires: []
provides:
  - HttpClient interface with fetchJsonArrayResult, postJsonResult, postJsonArrayResult methods
  - DefaultHttpClient implementations of all 3 new HttpResult-returning methods
  - FakeHttpClient supports all 3 new methods with configurable stubs and auto-fallback
  - WikidataApi, WikipediaApi, WikipediaProvider, FanartTvApi, ITunesApi migrated from fetchJson to fetchJsonResult
  - WikidataProvider, WikipediaProvider, FanartTvProvider, ITunesProvider emit ErrorKind.NETWORK/PARSE on errors
affects: [06-02, 06-03, all future provider plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "HttpResult migration: Api classes use fetchJsonResult; else-branch returns null; Provider catches Exception and maps to ErrorKind"
    - "mapError() helper in each Provider: IOException->NETWORK, JSONException->PARSE, else->UNKNOWN"
    - "FakeHttpClient.givenIoException(): throws IOException from fetchJsonResult to test Provider error handling"

key-files:
  created: []
  modified:
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/http/HttpClient.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/http/DefaultHttpClient.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/testutil/FakeHttpClient.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataProvider.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaProvider.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProvider.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesApi.kt
    - musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProvider.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/wikidata/WikidataProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/wikipedia/WikipediaProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/fanarttv/FanartTvProviderTest.kt
    - musicmeta-core/src/test/kotlin/com/landofoz/musicmeta/provider/itunes/ITunesProviderTest.kt

key-decisions:
  - "Api classes keep nullable return types; HttpResult error branches convert to null (not returned to callers). ErrorKind propagation happens at the Provider level via try/catch of IOException/JSONException"
  - "Added givenIoException() to FakeHttpClient (separate from givenError()) to test Provider-level error handling — givenError() returns null/NetworkError from fetchJson while givenIoException() throws IOException from fetchJsonResult, allowing the exception to propagate through the Api and be caught by the Provider's mapError()"

patterns-established:
  - "HttpResult migration pattern: replace httpClient.fetchJson(url) with when(val r = httpClient.fetchJsonResult(url)) { is HttpResult.Ok -> r.body; else -> return null/emptyList() }"
  - "Provider error mapping: private fun mapError(type, e) = ErrorKind-aware EnrichmentResult.Error"
  - "Error test pattern: givenIoException(urlPattern) in test -> IOException propagates through Api -> caught by Provider -> ErrorKind.NETWORK"

requirements-completed: [DEBT-01, DEBT-02]

# Metrics
duration: 8min
completed: 2026-03-21
---

# Phase 6 Plan 01: HttpResult Infrastructure and First 4 Provider Migration Summary

**HttpResult-returning method variants added to HttpClient (fetchJsonArrayResult, postJsonResult, postJsonArrayResult) and 4 providers fully migrated from nullable fetchJson to typed fetchJsonResult with ErrorKind error classification**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-21T15:57:14Z
- **Completed:** 2026-03-21T16:05:11Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- Extended HttpClient interface with 3 new HttpResult-returning methods; DefaultHttpClient implements all 3 with proper 429/4xx/5xx/2xx/JSONException/IOException status mapping
- Migrated WikidataApi, WikipediaApi (2 calls), WikipediaProvider (resolveFromWikidata), FanartTvApi, and ITunesApi from bare fetchJson to fetchJsonResult
- Added mapError() private helper to all 4 Providers: IOException->NETWORK, JSONException->PARSE, else->UNKNOWN; added error-path unit test for each

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend HttpClient with HttpResult-returning method variants** - `a0c92bf` (feat)
2. **Task 2: Migrate Wikidata, Wikipedia, FanartTv, iTunes to HttpResult/ErrorKind** - `e8b9d71` (feat)

## Files Created/Modified
- `http/HttpClient.kt` - Added fetchJsonArrayResult, postJsonResult, postJsonArrayResult to interface
- `http/DefaultHttpClient.kt` - Implemented all 3 new methods following fetchJsonResult pattern
- `testutil/FakeHttpClient.kt` - Added httpResultArrayResponses map, givenHttpResultArray(), givenIoException(), and 3 new override methods with error/fallback support
- `provider/wikidata/WikidataApi.kt` - fetchJson -> fetchJsonResult in getEntityProperties
- `provider/wikidata/WikidataProvider.kt` - Added try/catch + mapError()
- `provider/wikipedia/WikipediaApi.kt` - fetchJson -> fetchJsonResult in getPageSummary and getPageMediaList
- `provider/wikipedia/WikipediaProvider.kt` - fetchJson -> fetchJsonResult in resolveFromWikidata; added try/catch + mapError()
- `provider/fanarttv/FanartTvApi.kt` - fetchJson -> fetchJsonResult in getArtistImages
- `provider/fanarttv/FanartTvProvider.kt` - Updated catch block to use mapError()
- `provider/itunes/ITunesApi.kt` - fetchJson -> fetchJsonResult in searchAlbums
- `provider/itunes/ITunesProvider.kt` - Updated catch block to use mapError()
- 4 Provider test files - Added ErrorKind assertion test per provider

## Decisions Made
- Api classes keep nullable return types: HttpResult error branches convert to null. This preserves the existing interface shape and keeps the migration incremental — the Provider try/catch handles actual IOException propagation, not HttpResult values.
- Added `givenIoException()` to FakeHttpClient separate from `givenError()` because the error path needs an actual IOException to propagate through the Api layer to the Provider's catch block. `givenError()` (which makes fetchJson return null) doesn't trigger Provider-level catch blocks since the Api converts all non-Ok results to null before returning.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added givenIoException() to FakeHttpClient**
- **Found during:** Task 2 (error-path test implementation)
- **Issue:** Plan instructed using givenError() for error tests, but givenError() causes Api to return null (NotFound path), not Exception (Error path). Tests with givenError() would assert NotFound, not ErrorKind.NETWORK.
- **Fix:** Added givenIoException() to FakeHttpClient that throws IOException from fetchJsonResult. Updated all 4 error tests to use givenIoException() instead of givenError().
- **Files modified:** FakeHttpClient.kt and all 4 provider test files
- **Verification:** All 4 error tests pass with ErrorKind.NETWORK assertion
- **Committed in:** e8b9d71 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (missing critical - test infrastructure)
**Impact on plan:** Required for tests to actually verify ErrorKind propagation. No scope creep.

## Issues Encountered
- `givenError()` sets up null returns from fetchJson, but fetchJsonResult now has its own error path. Updated FakeHttpClient to check `ioExceptions` set first in all Result-returning methods, short-circuiting with an IOException before the normal error/response lookup.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- HttpResult infrastructure established; remaining 7 providers (MusicBrainz, Last.fm, CoverArtArchive, Deezer, Discogs, ListenBrainz, LRCLIB) ready for migration in Plan 06-02
- mapError() pattern established; future provider migrations should follow the same approach
- givenIoException() available for all future Provider error-path tests

---
*Phase: 06-tech-debt-cleanup*
*Completed: 2026-03-21*
