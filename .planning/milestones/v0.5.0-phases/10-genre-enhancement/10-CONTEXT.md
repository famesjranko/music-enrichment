# Phase 10: Genre Enhancement - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning
**Source:** PRD v0.5.0 Phase 4 + Smart Discuss (autonomous)

<domain>
## Phase Boundary

Genre results carry per-tag confidence scores via GenreTag, and the provider chain merges tags from all providers rather than short-circuiting on first success. Backward compatible — existing genres list still populated.

</domain>

<decisions>
## Implementation Decisions

### Data Model
- GenreTag data class as top-level @Serializable type in EnrichmentData.kt
- GenreTag fields: name (String), confidence (Float 0.0-1.0), sources (List<String>)
- Add genreTags: List<GenreTag>? = null to EnrichmentData.Metadata (alongside existing genres: List<String>?)
- Both fields populated — genres gets top-N tag names (backward compatible), genreTags gets full scored list

### GenreMerger (new file: engine/GenreMerger.kt)
- Normalize: lowercase, trim, map common aliases ("alt rock" → "alternative rock", "hip hop" → "hip-hop")
- Deduplicate: group by normalized name
- Score: Each provider vote adds confidence:
  - MusicBrainz tag with vote count: +0.4
  - Last.fm top tag: +0.3
  - Discogs genre: +0.3 (curated taxonomy)
  - Discogs style: +0.2 (more specific)
  - iTunes primaryGenre: +0.2
  - Multiple sources agreeing: cap at 1.0
- Rank: Sort by combined confidence desc
- Pure function: takes list of (provider_name, genreTags) → merged List<GenreTag>

### Mergeable Types in ProviderChain
- New concept: mergeableTypes set in ProviderChain or ProviderRegistry
- For mergeable types: collect ALL Success results from ALL providers instead of short-circuiting on first
- GENRE is the first (and currently only) mergeable type
- After collecting all results, pass to GenreMerger.merge()
- PRD Approach A (engine-level merge) recommended over Approach B (dedicated GenreProvider)

### Mapper Updates
- Each provider's mapper should populate genreTags on Metadata with per-provider confidence:
  - MusicBrainzMapper.toGenre() → set confidence from tag vote count
  - LastFmMapper.toGenre() → set confidence from position in tag list
  - DiscogsMapper (album metadata) → genres at 0.3, styles at 0.2
  - ITunesMapper → primaryGenre at 0.2

### Claude's Discretion
- How to wire mergeable types into ProviderChain (flag on chain vs registry)
- Genre alias map specifics
- How many genres to put in the backward-compatible genres list (top 5? top 10?)
- Where exactly the merge happens (engine level vs chain level)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- ProviderChain.resolve() — currently short-circuits on first Success
- ProviderRegistry builds chains per type
- DefaultEnrichmentEngine.resolveTypes() — fan-out per type
- Existing genre providers: MusicBrainz, Last.fm (already fetch genre data)
- Discogs and iTunes also return genre info but currently only in ALBUM_METADATA

### Established Patterns
- Engine resolves types concurrently
- Provider chains try providers in order, Success short-circuits (needs modification for mergeable)
- Metadata data class has nullable genre fields

### Integration Points
- EnrichmentData.kt — add GenreTag class, add genreTags to Metadata
- ProviderChain.kt — modify resolve() for mergeable types
- New: engine/GenreMerger.kt
- MusicBrainz/LastFm mappers — add genreTags to existing genre mapping

</code_context>

<specifics>
## Specific Ideas

PRD notes: "No new API calls needed — all genre data is already fetched." The change is in how the engine merges results. Currently the chain short-circuits — this needs to change for GENRE type specifically.

</specifics>

<deferred>
## Deferred Ideas

- Tag weighting by MusicBrainz vote count (could be v2 enhancement)
- Genre taxonomy hierarchy (parent/child genre relationships)
- Last.fm tag.gettopartists for "top artists in genre" feature

</deferred>
