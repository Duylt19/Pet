package com.asianmobile.emojibattery.shimeji.data.remote

internal object PetCatalogRefreshPolicy {
    const val REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1_000L
    private const val DEFAULT_RATE_LIMIT_DELAY_MILLIS = 60L * 60L * 1_000L
    private const val MAX_RATE_LIMIT_DELAY_MILLIS = 24L * 60L * 60L * 1_000L

    fun shouldRefresh(
        nowEpochMillis: Long,
        lastValidatedAtEpochMillis: Long,
        retryAfterEpochMillis: Long
    ): Boolean {
        if (nowEpochMillis < retryAfterEpochMillis) return false
        if (lastValidatedAtEpochMillis <= 0L || lastValidatedAtEpochMillis > nowEpochMillis) {
            return true
        }
        return nowEpochMillis - lastValidatedAtEpochMillis >= REFRESH_INTERVAL_MILLIS
    }

    fun rateLimitRetryAt(
        nowEpochMillis: Long,
        retryAfterSeconds: String?,
        rateLimitResetEpochSeconds: String?
    ): Long {
        val retryAfterCandidate = retryAfterSeconds
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it >= 0L }
            ?.let { seconds -> safeAdd(nowEpochMillis, safeMultiply(seconds, 1_000L)) }
        val resetCandidate = rateLimitResetEpochSeconds
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it >= 0L }
            ?.let { seconds -> safeMultiply(seconds, 1_000L) }
        val serverCandidate = listOfNotNull(retryAfterCandidate, resetCandidate)
            .maxOrNull()
            ?.coerceAtLeast(nowEpochMillis)
        val candidate = serverCandidate
            ?: safeAdd(nowEpochMillis, DEFAULT_RATE_LIMIT_DELAY_MILLIS)
        return candidate.coerceAtMost(safeAdd(nowEpochMillis, MAX_RATE_LIMIT_DELAY_MILLIS))
    }

    private fun safeAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    private fun safeMultiply(left: Long, right: Long): Long =
        if (left > 0L && right > Long.MAX_VALUE / left) Long.MAX_VALUE else left * right
}
