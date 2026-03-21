package com.landofoz.musicmeta.provider.fanarttv

import com.landofoz.musicmeta.EnrichmentData
import com.landofoz.musicmeta.EnrichmentIdentifiers
import com.landofoz.musicmeta.EnrichmentRequest
import com.landofoz.musicmeta.EnrichmentResult
import com.landofoz.musicmeta.EnrichmentType
import com.landofoz.musicmeta.http.RateLimiter
import com.landofoz.musicmeta.testutil.FakeHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FanartTvProviderTest {

    private lateinit var httpClient: FakeHttpClient
    private lateinit var provider: FanartTvProvider

    @Before
    fun setUp() {
        httpClient = FakeHttpClient()
        provider = FanartTvProvider(
            projectKey = "test-project-key",
            httpClient = httpClient,
            rateLimiter = RateLimiter(0L),
        )
    }

    @Test
    fun `enrich returns artist photo`() = runTest {
        // Given
        httpClient.givenJsonResponse("fanart.tv", ARTIST_IMAGES_JSON)
        val request = artistRequest()

        // When
        val result = provider.enrich(request, EnrichmentType.ARTIST_PHOTO)

        // Then
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data
        assertTrue(data is EnrichmentData.Artwork)
        assertEquals(
            "https://assets.fanart.tv/fanart/thumb1.jpg",
            (data as EnrichmentData.Artwork).url,
        )
    }

    @Test
    fun `enrich returns artist background`() = runTest {
        // Given
        httpClient.givenJsonResponse("fanart.tv", ARTIST_IMAGES_JSON)
        val request = artistRequest()

        // When
        val result = provider.enrich(request, EnrichmentType.ARTIST_BACKGROUND)

        // Then
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data
        assertTrue(data is EnrichmentData.Artwork)
        assertEquals(
            "https://assets.fanart.tv/fanart/bg1.jpg",
            (data as EnrichmentData.Artwork).url,
        )
    }

    @Test
    fun `enrich returns artist logo`() = runTest {
        // Given
        httpClient.givenJsonResponse("fanart.tv", ARTIST_IMAGES_JSON)
        val request = artistRequest()

        // When
        val result = provider.enrich(request, EnrichmentType.ARTIST_LOGO)

        // Then
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data
        assertTrue(data is EnrichmentData.Artwork)
        assertEquals(
            "https://assets.fanart.tv/fanart/logo1.png",
            (data as EnrichmentData.Artwork).url,
        )
    }

    @Test
    fun `enrich returns NotFound when no MBID`() = runTest {
        // Given - no musicBrainzId in identifiers
        val request = EnrichmentRequest.ForArtist(
            identifiers = EnrichmentIdentifiers(),
            name = "Radiohead",
        )

        // When
        val result = provider.enrich(request, EnrichmentType.ARTIST_BACKGROUND)

        // Then
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound when API returns no images`() = runTest {
        // Given - empty image arrays
        httpClient.givenJsonResponse("fanart.tv", EMPTY_IMAGES_JSON)
        val request = artistRequest()

        // When
        val result = provider.enrich(request, EnrichmentType.ARTIST_BACKGROUND)

        // Then
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound when API key is blank`() = runTest {
        // Given
        val blankProvider = FanartTvProvider(
            projectKey = "",
            httpClient = httpClient,
            rateLimiter = RateLimiter(0L),
        )
        val request = artistRequest()

        // When
        val result = blankProvider.enrich(request, EnrichmentType.ARTIST_PHOTO)

        // Then
        assertTrue(result is EnrichmentResult.NotFound)
    }

    private fun artistRequest() = EnrichmentRequest.ForArtist(
        identifiers = EnrichmentIdentifiers(
            musicBrainzId = "a74b1b7f-71a5-4011-9441-d0b5e4122711",
        ),
        name = "Radiohead",
    )

    @Test
    fun `enrich returns NotFound when image objects have no url field`() = runTest {
        // Given — Fanart.tv returns image arrays with objects missing the url field
        httpClient.givenJsonResponse("fanart.tv", """{
            "artistthumb": [{"id": "12345", "likes": "3"}],
            "artistbackground": [{"id": "67890"}],
            "hdmusiclogo": []
        }""")
        val request = artistRequest()

        // When — enriching for artist photo
        val result = provider.enrich(request, EnrichmentType.ARTIST_PHOTO)

        // Then — NotFound because extractUrls filters out objects without a valid url
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns NotFound when albums key has no album objects`() = runTest {
        // Given — Fanart.tv returns an albums object that is empty (no album MBIDs)
        httpClient.givenJsonResponse("fanart.tv", """{
            "artistthumb": [],
            "artistbackground": [],
            "hdmusiclogo": [],
            "musicbanner": [],
            "albums": {}
        }""")
        val request = artistRequest()

        // When — enriching for album art
        val result = provider.enrich(request, EnrichmentType.ALBUM_ART)

        // Then — NotFound because albumCovers list is empty
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns artist banner`() = runTest {
        // Given — Fanart.tv returns images with a banner URL
        httpClient.givenJsonResponse("fanart.tv", BANNER_IMAGES_JSON)
        val request = artistRequest()

        // When — enriching for artist banner
        val result = provider.enrich(request, EnrichmentType.ARTIST_BANNER)

        // Then — success with banner artwork
        assertTrue(result is EnrichmentResult.Success)
        val data = (result as EnrichmentResult.Success).data
        assertTrue(data is EnrichmentData.Artwork)
        assertEquals(
            "https://assets.fanart.tv/fanart/banner1.jpg",
            (data as EnrichmentData.Artwork).url,
        )
    }

    @Test
    fun `enrich returns NotFound when no banners`() = runTest {
        // Given — Fanart.tv returns images with empty banners
        httpClient.givenJsonResponse("fanart.tv", EMPTY_IMAGES_JSON)
        val request = artistRequest()

        // When — enriching for artist banner
        val result = provider.enrich(request, EnrichmentType.ARTIST_BANNER)

        // Then — NotFound because no banner images available
        assertTrue(result is EnrichmentResult.NotFound)
    }

    @Test
    fun `enrich returns artwork with sizes when multiple images exist`() = runTest {
        // Given -- Fanart.tv returns multiple thumbnails for an artist
        httpClient.givenJsonResponse("fanart.tv", MULTI_THUMB_JSON)
        val request = artistRequest()

        // When -- enriching for artist photo
        val result = provider.enrich(request, EnrichmentType.ARTIST_PHOTO)

        // Then -- artwork has sizes list with all image variants
        assertTrue(result is EnrichmentResult.Success)
        val artwork = (result as EnrichmentResult.Success).data as EnrichmentData.Artwork
        assertEquals("https://assets.fanart.tv/fanart/thumb1.jpg", artwork.url)
        assertNotNull(artwork.sizes)
        assertEquals(3, artwork.sizes!!.size)
    }

    @Test
    fun `enrich returns artwork without sizes when single image`() = runTest {
        // Given -- Fanart.tv returns a single thumbnail
        httpClient.givenJsonResponse("fanart.tv", ARTIST_IMAGES_JSON)
        val request = artistRequest()

        // When -- enriching for artist photo
        val result = provider.enrich(request, EnrichmentType.ARTIST_PHOTO)

        // Then -- artwork has no sizes (only 1 image, not worth listing)
        assertTrue(result is EnrichmentResult.Success)
        val artwork = (result as EnrichmentResult.Success).data as EnrichmentData.Artwork
        assertNull(artwork.sizes)
    }

    private companion object {
        val MULTI_THUMB_JSON = """
            {
              "artistthumb": [
                {"url": "https://assets.fanart.tv/fanart/thumb1.jpg", "id": "100", "likes": "5"},
                {"url": "https://assets.fanart.tv/fanart/thumb2.jpg", "id": "101", "likes": "3"},
                {"url": "https://assets.fanart.tv/fanart/thumb3.jpg", "id": "102", "likes": "1"}
              ],
              "artistbackground": [],
              "hdmusiclogo": []
            }
        """.trimIndent()

        val BANNER_IMAGES_JSON = """
            {
              "artistthumb": [],
              "artistbackground": [],
              "hdmusiclogo": [],
              "musicbanner": [{"url": "https://assets.fanart.tv/fanart/banner1.jpg"}]
            }
        """.trimIndent()

        val ARTIST_IMAGES_JSON = """
            {
              "artistthumb": [{"url": "https://assets.fanart.tv/fanart/thumb1.jpg"}],
              "artistbackground": [{"url": "https://assets.fanart.tv/fanart/bg1.jpg"}],
              "hdmusiclogo": [{"url": "https://assets.fanart.tv/fanart/logo1.png"}]
            }
        """.trimIndent()

        val EMPTY_IMAGES_JSON = """
            {
              "artistthumb": [],
              "artistbackground": [],
              "hdmusiclogo": [],
              "musicbanner": []
            }
        """.trimIndent()
    }
}
