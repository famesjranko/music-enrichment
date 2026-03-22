# Phase 8: Release Editions - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning
**Source:** PRD v0.5.0 Phase 2 + Smart Discuss (autonomous)

<domain>
## Phase Boundary

New RELEASE_EDITIONS enrichment type enabling consumers to list all editions/pressings of an album (original, deluxe, remaster, vinyl, CD, regional variants) from MusicBrainz release-group releases and Discogs master versions. ForAlbum requests only.

</domain>

<decisions>
## Implementation Decisions

### Data Model
- ReleaseEditions and ReleaseEdition data classes as top-level @Serializable types in EnrichmentData.kt
- ReleaseEdition fields: title, format (nullable), country (nullable), year (nullable Int), label (nullable), catalogNumber (nullable), barcode (nullable), identifiers (EnrichmentIdentifiers)
- RELEASE_EDITIONS TTL: 1 year (365L * 24 * 60 * 60 * 1000) — editions rarely change
- ForAlbum requests only

### MusicBrainz Editions
- New API: lookupReleaseGroup(releaseGroupMbid) — GET /ws/2/release-group/{mbid}?inc=releases
- Parse releases[] array with format from media[].format, label from label-info[].label.name, catalog number from label-info[].catalog-number
- New model: MusicBrainzReleaseGroupDetail, MusicBrainzEdition
- New parser: parseReleaseGroupDetail(json) → MusicBrainzReleaseGroupDetail
- New mapper: MusicBrainzMapper.toReleaseEditions()
- Priority 100, identifierRequirement = MUSICBRAINZ_ID (needs release-group MBID from identity resolution)
- Note: release-group MBID comes from musicBrainzReleaseGroupId on EnrichmentIdentifiers (already populated by MusicBrainz identity resolution)

### Discogs Editions
- New API: getMasterVersions(masterId) — GET /masters/{master_id}/versions
- Requires discogsMasterId from Phase 6 DEBT-04 (stored in identifiers.extra)
- New model: DiscogsMasterVersion (id, title, format, label, country, year, catno)
- New mapper: DiscogsMapper.toReleaseEditions()
- Priority 50, fallback behind MusicBrainz

### Claude's Discretion
- Internal implementation details for parser/mapper structure
- Test fixture JSON structure
- Error handling within established mapError() pattern

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 7 established the pattern: new type + MusicBrainz primary + Discogs fallback
- MusicBrainzParser existing methods (parseBandMembers, parseRecordingCredits) as pattern templates
- DiscogsApi.getReleaseDetails() from Phase 7 — similar endpoint pattern for getMasterVersions
- ConfidenceCalculator for confidence scoring
- FakeHttpClient with givenIoException() for error tests

### Established Patterns
- Provider: Api/Models/Parser/Mapper/Provider structure
- New types: add to EnrichmentType enum, sealed subclass in EnrichmentData
- Serialization: @Serializable on all data classes, round-trip tests
- Discogs IDs: read from identifiers.extra map (pattern from Phase 6/7)

### Integration Points
- EnrichmentType.kt — add RELEASE_EDITIONS enum value
- EnrichmentData.kt — add ReleaseEditions, ReleaseEdition data classes
- MusicBrainzApi/Models/Parser/Mapper/Provider — new release-group endpoint + editions capability
- DiscogsApi/Models/Mapper/Provider — new master versions endpoint + editions capability

</code_context>

<specifics>
## Specific Ideas

No specific requirements beyond PRD Phase 2 specification.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>
