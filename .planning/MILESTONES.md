# Milestones

## v0.4.0 Provider Abstraction Overhaul (Shipped: 2026-03-21)

**Phases completed:** 5 phases, 15 plans, 31 tasks

**Key accomplishments:**

- Fix MusicBrainz empty-result misclassification to NotFound, Last.fm HTTP-to-HTTPS, and TRACK_POPULARITY removal with 6 new TDD tests
- Fixed LRCLIB duration truncation (Int to Double) and Wikidata preferred-rank P18 claim selection, verified by 5 new TDD tests
- IdentifierRequirement enum with 6 typed values replacing boolean requiresIdentifier, plus isIdentityProvider flag and data-driven needsIdentityResolution
- Removed IdentifierResolution sealed subclass from public API; MusicBrainzProvider returns Metadata directly with resolved IDs on Success.resolvedIdentifiers, engine uses provider.resolveIdentity() for the identity resolution pathway
- Extracted 11 mapper objects isolating DTO-to-EnrichmentData mapping from provider logic, plus ApiKeyConfig with Builder.withDefaultProviders() for one-line engine setup
- TTL moved into EnrichmentType enum entries with config overrides, extensible extra identifier map on EnrichmentIdentifiers, SimilarArtist/PopularTrack migrated to use EnrichmentIdentifiers
- ErrorKind enum on EnrichmentResult.Error and HttpResult sealed class with typed HTTP responses via fetchJsonResult()
- 6 new EnrichmentType enum values, 5 sealed subclasses, 6 supporting data classes, ArtworkSize with sizes field on Artwork, all with serialization round-trip tests
- MusicBrainz provider expanded from 5 to 9 capabilities with band members from artist-rels, discography from browse endpoint, tracklist from media array, and artist links from url-rels
- Deezer discography/tracks, Last.fm similar tracks, and Fanart.tv banner capabilities with search-then-fetch pattern and 7 new unit tests
- All 4 artwork providers emit sizes, Wikidata returns expanded properties (birth/death/country/occupation), Discogs returns band members via artist endpoint
- ALBUM_ART_BACK and ALBUM_BOOKLET enrichment types backed by Cover Art Archive JSON metadata endpoint with image type filtering
- Last.fm TRACK_POPULARITY restored via track.getInfo with track-level playcount/listeners; ListenBrainz batch POST endpoints for recording and artist popularity with top-recordings fallback
- ALBUM_METADATA type served by Deezer (priority 50), iTunes (30), Discogs (40) mining previously ignored search fields; Wikipedia ARTIST_PHOTO via page media-list as supplemental source (priority 30)
- ConfidenceCalculator utility with 4 semantic scoring methods replacing hardcoded floats across all 11 providers

---
