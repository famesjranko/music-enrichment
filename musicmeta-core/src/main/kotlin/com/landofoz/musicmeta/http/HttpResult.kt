package com.landofoz.musicmeta.http

/**
 * Typed HTTP response that preserves status information for callers.
 * Unlike the nullable returns of [HttpClient.fetchJson], this captures
 * the specific failure mode so providers can react differently to
 * 404 (not found) vs 429 (rate limited) vs 500 (server error).
 */
sealed class HttpResult<out T> {
    data class Ok<T>(val body: T, val statusCode: Int = 200) : HttpResult<T>()
    data class ClientError(val statusCode: Int, val body: String? = null) : HttpResult<Nothing>()
    data class ServerError(val statusCode: Int, val body: String? = null) : HttpResult<Nothing>()
    data class RateLimited(val retryAfterMs: Long? = null) : HttpResult<Nothing>()
    data class NetworkError(val message: String, val cause: Throwable? = null) : HttpResult<Nothing>()
}
