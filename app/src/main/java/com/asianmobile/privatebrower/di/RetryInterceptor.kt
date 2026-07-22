package com.asianmobile.privatebrower.di

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Retries transient failures for safe (GET/HEAD) requests — the only kind the download engine
 * issues. A flaky CDN often returns a 5xx or drops the socket on the first hit but succeeds on a
 * retry, so this keeps a whole video download from failing on a momentary blip.
 *
 * Non-idempotent methods, client errors (4xx), and successful responses pass straight through.
 * Range requests are safe to retry: the server re-serves the same byte range.
 */
class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.method.equals("GET", true) && !request.method.equals("HEAD", true)) {
            return chain.proceed(request)
        }

        var lastError: IOException? = null
        var attempt = 0
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                // Retry only server-side transient errors; 4xx are the caller's problem.
                if (response.code in TRANSIENT_CODES && attempt < maxRetries) {
                    response.close()
                    backoff(attempt)
                    attempt++
                    continue
                }
                return response
            } catch (e: IOException) {
                lastError = e
                if (attempt >= maxRetries) break
                backoff(attempt)
                attempt++
            }
        }
        throw lastError ?: IOException("Request failed after $maxRetries retries")
    }

    private fun backoff(attempt: Int) {
        try {
            Thread.sleep(BASE_BACKOFF_MS * (attempt + 1))
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        private val TRANSIENT_CODES = setOf(500, 502, 503, 504)
        private const val BASE_BACKOFF_MS = 500L
    }
}
