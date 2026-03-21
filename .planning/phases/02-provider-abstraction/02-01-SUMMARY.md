---
phase: 02-provider-abstraction
plan: 01
subsystem: api
tags: [kotlin, enrichment-provider, identifier-requirements, identity-provider, provider-chain]

# Dependency graph
requires:
  - phase: 01-bug-fixes
    provides: "Working provider implementations with correct API handling"
provides:
  - "IdentifierRequirement enum with 6 typed values replacing boolean requiresIdentifier"
  - "isIdentityProvider flag on EnrichmentProvider interface"
  - "resolveIdentity() default method on EnrichmentProvider interface"
  - "Data-driven needsIdentityResolution scanning provider capabilities"
  - "providers() accessor on ProviderChain"
affects: [02-provider-abstraction, 03-public-api-cleanup]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Typed identifier requirements via enum instead of boolean"
    - "Identity provider selection by explicit flag instead of capability heuristic"
    - "Data-driven engine decisions from provider capability declarations"

key-files:
  created: []
  modified:
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/EnrichmentProvider.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/ProviderChain.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/ProviderRegistry.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/engine/DefaultEnrichmentEngine.kt"
    - "musicmeta-core/src/main/kotlin/com/landofoz/musicmeta/provider/*/Provider.kt (all 11)"

key-decisions:
  - "MUSICBRAINZ_ID chosen as primary requirement for CoverArtArchive (handles releaseGroupId fallback internally)"
  - "WIKIPEDIA_TITLE requirement accepts wikidataId because WikipediaProvider resolves titles from Wikidata sitelinks"
  - "Keep 'no MBID -> always resolve' check in needsIdentityResolution for backward compatibility with metadata extraction"
  - "IDENTITY_TYPES set retained in DefaultEnrichmentEngine (used by resolveIdentity for metadata extraction, removed in Plan 02)"

patterns-established:
  - "IdentifierRequirement enum: typed provider identifier needs"
  - "isIdentityProvider flag: explicit identity provider role"
  - "Data-driven engine heuristics: scan capabilities not hardcode types"

requirements-completed: [ABST-01, ABST-02, ABST-06]

# Metrics
duration: 10min
completed: 2026-03-21
---

# Phase 02 Plan 01: Typed Identifier Requirements Summary

**IdentifierRequirement enum with 6 typed values replacing boolean requiresIdentifier, plus isIdentityProvider flag and data-driven needsIdentityResolution**

## Performance

- **Duration:** 10 min
- **Started:** 2026-03-21T07:58:41Z
- **Completed:** 2026-03-21T08:09:10Z
- **Tasks:** 3
- **Files modified:** 18

## Accomplishments
- Replaced boolean `requiresIdentifier` with typed `IdentifierRequirement` enum (NONE, MUSICBRAINZ_ID, MUSICBRAINZ_RELEASE_GROUP_ID, WIKIDATA_ID, WIKIPEDIA_TITLE, ANY_IDENTIFIER)
- ProviderChain now checks specific identifier fields per requirement type instead of any-identifier-present
- MusicBrainzProvider formalized as identity provider via `isIdentityProvider` flag with `resolveIdentity()` override
- ProviderRegistry selects identity provider by flag instead of GENRE/LABEL heuristic
- needsIdentityResolution scans provider capability declarations instead of hardcoded type list

## Task Commits

Each task was committed atomically:

1. **Task 1: Add IdentifierRequirement enum, update ProviderCapability, identity provider interface** - `6387921` (test) + `957004a` (feat)
2. **Task 2: Update all 11 providers with typed identifier requirements** - `8c94865` (feat)
3. **Task 3: Make needsIdentityResolution data-driven** - `ba0add9` (test) + `53d4403` (feat)

_Note: TDD tasks have two commits each (test then feat)_

## Files Created/Modified
- `EnrichmentProvider.kt` - Added IdentifierRequirement enum, identifierRequirement param, isIdentityProvider, resolveIdentity()
- `ProviderChain.kt` - Rewritten hasRequiredIdentifiers with when-expression on enum, added providers() accessor
- `ProviderRegistry.kt` - identityProvider() now uses isIdentityProvider flag
- `DefaultEnrichmentEngine.kt` - Data-driven needsIdentityResolution scanning chain capabilities
- `MusicBrainzProvider.kt` - isIdentityProvider=true, resolveIdentity() override
- `WikidataProvider.kt` - WIKIDATA_ID requirement
- `WikipediaProvider.kt` - WIKIPEDIA_TITLE requirement
- `FanartTvProvider.kt` - MUSICBRAINZ_ID on all 5 capabilities
- `CoverArtArchiveProvider.kt` - MUSICBRAINZ_ID requirement
- `ListenBrainzProvider.kt` - MUSICBRAINZ_ID requirement
- `DeezerProvider.kt, ITunesProvider.kt, LrcLibProvider.kt` - Removed explicit requiresIdentifier=false
- `LastFmProvider.kt, DiscogsProvider.kt` - No change needed (already omitted requiresIdentifier)
- `FakeProvider.kt` - Added isIdentityProvider parameter
- `ProviderChainTest.kt` - Updated and added tests for typed requirements
- `ProviderRegistryTest.kt` - Added identity provider flag tests
- `DefaultEnrichmentEngineTest.kt` - Added data-driven tests, updated identity provider references

## Decisions Made
- CoverArtArchive uses MUSICBRAINZ_ID (not a new MUSICBRAINZ_RELEASE_GROUP_ID) because the provider handles releaseGroupId fallback internally
- WIKIPEDIA_TITLE requirement also accepts wikidataId in ProviderChain because WikipediaProvider can resolve titles via Wikidata sitelinks
- Kept the "no MBID at all -> always resolve" initial check in needsIdentityResolution for backward compatibility (identity resolution provides both identifiers AND metadata as side-effects)
- IDENTITY_TYPES companion set retained in DefaultEnrichmentEngine (still used by resolveIdentity() for metadata extraction; Plan 02 will address this when removing IdentifierResolution)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Provider updates pulled into Task 1 GREEN phase**
- **Found during:** Task 1 (TDD GREEN)
- **Issue:** Gradle compiles all source before test source; changing ProviderCapability broke all 11 provider files
- **Fix:** Applied provider identifier requirement updates (Task 2 scope) during Task 1 GREEN to achieve compilation
- **Files modified:** All 11 provider files
- **Verification:** Full test suite passes
- **Committed in:** Provider changes committed separately as Task 2 (8c94865)

**2. [Rule 1 - Bug] Updated DefaultEnrichmentEngineTest identity provider references**
- **Found during:** Task 1 (TDD GREEN)
- **Issue:** Existing tests used FakeProvider with GENRE capability as implicit identity provider (old heuristic). With flag-based selection, those tests silently broke
- **Fix:** Added isIdentityProvider=true to all FakeProvider instances used as identity providers in tests
- **Files modified:** DefaultEnrichmentEngineTest.kt
- **Verification:** All 22 DefaultEnrichmentEngineTest tests pass

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both auto-fixes necessary for compilation and test correctness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Provider abstraction layer has typed identifier requirements in place
- Plan 02 (mapper pattern, IdentifierResolution removal) can build on this foundation
- Plan 03 (API key management) is independent and can proceed in parallel

## Self-Check: PASSED

- All 6 key files verified present
- All 5 task commits verified in git log

---
*Phase: 02-provider-abstraction*
*Completed: 2026-03-21*
