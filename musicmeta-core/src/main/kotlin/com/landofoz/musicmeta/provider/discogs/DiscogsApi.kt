package com.landofoz.musicmeta.provider.discogs

import com.landofoz.musicmeta.http.HttpClient
import com.landofoz.musicmeta.http.HttpResult
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
        val url = "$SEARCH_URL?type=release&title=$encodedTitle" +
            "&artist=$encodedArtist&per_page=$limit&token=${tokenProvider()}"
        val json = rateLimiter.execute {
            when (val r = httpClient.fetchJsonResult(url)) {
                is HttpResult.Ok -> r.body
                else -> return@execute null
            }
        } ?: return emptyList()
        return parseReleaseResults(json)
    }

    /** Search for an artist by name and return the first match's Discogs ID. */
    suspend fun searchArtist(name: String): Long? {
        val encoded = URLEncoder.encode(name, "UTF-8")
        val url = "$SEARCH_URL?type=artist&q=$encoded&per_page=1&token=${tokenProvider()}"
        val json = rateLimiter.execute {
            when (val r = httpClient.fetchJsonResult(url)) {
                is HttpResult.Ok -> r.body
                else -> return@execute null
            }
        } ?: return null
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val id = results.getJSONObject(0).optLong("id", 0L)
        return if (id > 0) id else null
    }

    /** Fetch artist details including band members. */
    suspend fun getArtist(artistId: Long): DiscogsArtist? {
        val url = "$ARTISTS_URL/$artistId?token=${tokenProvider()}"
        val json = rateLimiter.execute {
            when (val r = httpClient.fetchJsonResult(url)) {
                is HttpResult.Ok -> r.body
                else -> return@execute null
            }
        } ?: return null
        return parseArtist(json)
    }

    private fun parseArtist(json: JSONObject): DiscogsArtist {
        val members = mutableListOf<DiscogsMember>()
        val membersArray = json.optJSONArray("members")
        if (membersArray != null) {
            for (i in 0 until membersArray.length()) {
                val obj = membersArray.getJSONObject(i)
                members.add(
                    DiscogsMember(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", ""),
                        active = if (obj.has("active")) obj.optBoolean("active") else null,
                    ),
                )
            }
        }
        return DiscogsArtist(
            id = json.optLong("id", 0L),
            name = json.optString("name", ""),
            members = members,
        )
    }

    private fun parseReleaseResults(json: JSONObject): List<DiscogsRelease> {
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val obj = results.getJSONObject(i)
            val labels = obj.optJSONArray("label")
            val label = if (labels != null && labels.length() > 0) labels.getString(0) else null
            val genreArr = obj.optJSONArray("genre")
            val genres = genreArr?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            val styleArr = obj.optJSONArray("style")
            val styles = styleArr?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            DiscogsRelease(
                title = obj.optString("title", ""),
                label = label,
                year = obj.optString("year").takeIf { it.isNotBlank() },
                country = obj.optString("country").takeIf { it.isNotBlank() },
                coverImage = obj.optString("cover_image").takeIf { it.isNotBlank() },
                releaseType = obj.optString("type").takeIf { it.isNotBlank() },
                catno = obj.optString("catno").takeIf { it.isNotBlank() },
                genres = genres,
                styles = styles,
            )
        }
    }

    private companion object {
        const val SEARCH_URL = "https://api.discogs.com/database/search"
        const val ARTISTS_URL = "https://api.discogs.com/artists"
    }
}
