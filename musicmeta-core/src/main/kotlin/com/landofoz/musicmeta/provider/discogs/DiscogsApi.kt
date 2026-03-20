package com.landofoz.musicmeta.provider.discogs

import com.landofoz.musicmeta.http.HttpClient
import com.landofoz.musicmeta.http.RateLimiter
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Discogs API client. Requires a personal access token.
 * Rate limited to 60 requests/minute (1000ms interval).
 */
class DiscogsApi(
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
) {

    constructor(personalToken: String, httpClient: HttpClient, rateLimiter: RateLimiter) :
        this({ personalToken }, httpClient, rateLimiter)

    suspend fun searchReleases(title: String, artist: String, limit: Int = 5): List<DiscogsRelease> {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        val url = "$BASE_URL?type=release&title=$encodedTitle" +
            "&artist=$encodedArtist&per_page=$limit&token=${tokenProvider()}"
        val json = rateLimiter.execute { httpClient.fetchJson(url) } ?: return emptyList()
        return parseResults(json)
    }

    private fun parseResults(json: JSONObject): List<DiscogsRelease> {
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val obj = results.getJSONObject(i)
            val labels = obj.optJSONArray("label")
            val label = if (labels != null && labels.length() > 0) labels.getString(0) else null
            DiscogsRelease(
                title = obj.optString("title", ""),
                label = label,
                year = obj.optString("year").takeIf { it.isNotBlank() },
                country = obj.optString("country").takeIf { it.isNotBlank() },
                coverImage = obj.optString("cover_image").takeIf { it.isNotBlank() },
                releaseType = obj.optString("type").takeIf { it.isNotBlank() },
            )
        }
    }

    private companion object {
        const val BASE_URL = "https://api.discogs.com/database/search"
    }
}
