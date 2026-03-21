package com.landofoz.musicmeta.provider.discogs

import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentRequest
import com.landofoz.musicmeta.EnrichmentResult
import com.landofoz.musicmeta.EnrichmentType
import com.landofoz.musicmeta.http.RateLimiter
import com.landofoz.musicmeta.testutil.FakeHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiscogsProviderTest {

    private lateinit var httpClient: FakeHttpClient
    private lateinit var provider: DiscogsProvider

    @Before
    fun setUp() {
        httpClient = FakeHttpClient()
        provider = DiscogsProvider(
            personalToken = "test-token",
            httpClient = httpClient,
            rateLimiter = RateLimiter(0L),
        )
    }

    @Test
    fun `enrich returns album art from search`() = runTest {
        // Given
        httpClient.givenJsonResponse("discogs.com", SEARCH_RESULTS_JSON)
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When
        val result = provider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then
        assertTrue(result is EnrichmentResult.Success)
        val success = result as EnrichmentResult.Success
        val data = success.data
        assertTrue(data is EnrichmentData.Artwork)
        assertEquals(
            "https://img.discogs.com/cover.jpg",
            (data as EnrichmentData.Artwork).url,
        )
        assertEquals(0.6f, success.confidence)
    }

    @Test
    fun `enrich returns label from search`() = runTest {
        // Given
        httpClient.givenJsonResponse("discogs.com", SEARCH_RESULTS_JSON)
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When
        val result = provider.enrich(request, EnrichmentType.LABEL)

        // Then
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data
        assertTrue(data is EnrichmentData.Metadata)
        assertEquals("Parlophone", (data as EnrichmentData.Metadata).label)
    }

    @Test
    fun `enrich returns NotFound when no results`() = runTest {
        // Given
        httpClient.givenJsonResponse("discogs.com", EMPTY_RESULTS_JSON)
        val request = EnrichmentRequest.forAlbum(
            title = "Nonexistent Album",
            artist = "Unknown",
        )

        // When
        val result = provider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound for artist requests`() = runTest {
        // Given
        val request = EnrichmentRequest.forArtist(name = "Radiohead")

        // When
        val result = provider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound when API key is blank`() = runTest {
        // Given
        val blankProvider = DiscogsProvider(
            personalToken = "",
            httpClient = httpClient,
            rateLimiter = RateLimiter(0L),
        )
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When
        val result = blankProvider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound when results field is missing from JSON`() = runTest {
        // Given — Discogs API returns JSON without a "results" array
        httpClient.givenJsonResponse("discogs.com", """{"pagination":{"pages":0}}""")
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When — enriching for album art
        val result = provider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then — NotFound because optJSONArray("results") returns null
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound when first result has no cover_image field`() = runTest {
        // Given — Discogs result object is missing the cover_image field
        httpClient.givenJsonResponse("discogs.com", """{
            "results": [{
                "title": "Radiohead - OK Computer",
                "label": ["Parlophone"],
                "year": "1997"
            }]
        }""")
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When — enriching for album art
        val result = provider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then — NotFound because coverImage is null after takeIf check
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound for label when label array is empty`() = runTest {
        // Given — Discogs result has an empty label array
        httpClient.givenJsonResponse("discogs.com", """{
            "results": [{
                "title": "Radiohead - OK Computer",
                "label": [],
                "year": "1997",
                "cover_image": "https://img.discogs.com/cover.jpg"
            }]
        }""")
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When — enriching for label metadata
        val result = provider.enrich(request, EnrichmentType.LABEL)

        // Then — NotFound because label is null when array is empty
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns BandMembers for artist`() = runTest {
        // Given — Discogs returns artist search result and artist detail with members
        httpClient.givenJsonResponse("search?type=artist", ARTIST_SEARCH_JSON)
        httpClient.givenJsonResponse("artists/12345", ARTIST_DETAIL_JSON)
        val request = EnrichmentRequest.forArtist(name = "Radiohead")

        // When — enriching for band members
        val result = provider.enrich(request, EnrichmentType.BAND_MEMBERS)

        // Then — success with 2 band members
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data as EnrichmentData.BandMembers
        assertEquals(2, data.members.size)
        assertEquals("Thom Yorke", data.members[0].name)
        assertEquals("Jonny Greenwood", data.members[1].name)
    }

    @Test
    fun `enrich returns NotFound for BandMembers when artist has no members`() = runTest {
        // Given — Discogs returns artist with empty members list
        httpClient.givenJsonResponse("search?type=artist", ARTIST_SEARCH_JSON)
        httpClient.givenJsonResponse("artists/12345", ARTIST_NO_MEMBERS_JSON)
        val request = EnrichmentRequest.forArtist(name = "Solo Artist")

        // When — enriching for band members
        val result = provider.enrich(request, EnrichmentType.BAND_MEMBERS)

        // Then — NotFound because artist has no members
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound for BandMembers when artist search fails`() = runTest {
        // Given — Discogs artist search returns no results
        httpClient.givenJsonResponse("search?type=artist", """{"results":[]}""")
        val request = EnrichmentRequest.forArtist(name = "Unknown Band")

        // When — enriching for band members
        val result = provider.enrich(request, EnrichmentType.BAND_MEMBERS)

        // Then — NotFound because artist was not found
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns album metadata with catalog number, genres, styles`() = runTest {
        // Given — Discogs returns search results with extra metadata fields
        httpClient.givenJsonResponse("discogs.com", METADATA_SEARCH_JSON)
        val request = EnrichmentRequest.forAlbum(
            title = "OK Computer",
            artist = "Radiohead",
        )

        // When — enriching for album metadata
        val result = provider.enrich(request, EnrichmentType.ALBUM_METADATA)

        // Then — success with Metadata containing catalogNumber, genres, label, country
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data as EnrichmentData.Metadata
        assertEquals("NODATA 02", data.catalogNumber)
        assertEquals("Parlophone", data.label)
        assertEquals("UK", data.country)
        assertEquals("1997", data.releaseDate)
        assertTrue(data.genres!!.contains("Electronic"))
        assertTrue(data.genres!!.contains("Rock"))
        assertTrue(data.genres!!.contains("Art Rock"))
    }

    @Test
    fun `enrich returns NotFound for album metadata when no results`() = runTest {
        // Given — Discogs returns empty results
        httpClient.givenJsonResponse("discogs.com", EMPTY_RESULTS_JSON)
        val request = EnrichmentRequest.forAlbum(
            title = "Nonexistent",
            artist = "Nobody",
        )

        // When — enriching for album metadata
        val result = provider.enrich(request, EnrichmentType.ALBUM_METADATA)

        // Then — NotFound
        assertTrue(result is EnrichmentResult.NotFound)
    }

    private companion object {
        val METADATA_SEARCH_JSON = """
            {
              "results": [
                {
                  "title": "Radiohead - OK Computer",
                  "label": ["Parlophone"],
                  "year": "1997",
                  "country": "UK",
                  "cover_image": "https://img.discogs.com/cover.jpg",
                  "type": "release",
                  "catno": "NODATA 02",
                  "genre": ["Electronic", "Rock"],
                  "style": ["Art Rock"]
                }
              ]
            }
        """.trimIndent()

        val ARTIST_SEARCH_JSON = """
            {"results":[{"id":12345,"name":"Radiohead"}]}
        """.trimIndent()

        val ARTIST_DETAIL_JSON = """
            {
              "id": 12345,
              "name": "Radiohead",
              "members": [
                {"id": 100, "name": "Thom Yorke", "active": true},
                {"id": 101, "name": "Jonny Greenwood", "active": true}
              ]
            }
        """.trimIndent()

        val ARTIST_NO_MEMBERS_JSON = """
            {"id": 12345, "name": "Solo Artist", "members": []}
        """.trimIndent()

        val SEARCH_RESULTS_JSON = """
            {
              "results": [
                {
                  "title": "Radiohead - OK Computer",
                  "label": ["Parlophone"],
                  "year": "1997",
                  "country": "UK",
                  "cover_image": "https://img.discogs.com/cover.jpg"
                }
              ]
            }
        """.trimIndent()

        val EMPTY_RESULTS_JSON = """
            {
              "results": []
            }
        """.trimIndent()
    }
}
