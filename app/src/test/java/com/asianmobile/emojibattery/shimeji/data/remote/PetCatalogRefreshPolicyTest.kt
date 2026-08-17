package com.asianmobile.emojibattery.shimeji.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetCatalogRefreshPolicyTest {
    @Test
    fun `force refresh bypasses freshness but respects rate limit delay`() {
        assertTrue(
            PetCatalogRefreshPolicy.canForceRefresh(
                nowEpochMillis = 10_000L,
                retryAfterEpochMillis = 0L
            )
        )
        assertFalse(
            PetCatalogRefreshPolicy.canForceRefresh(
                nowEpochMillis = 10_000L,
                retryAfterEpochMillis = 10_001L
            )
        )
    }

    @Test
    fun `catalog without a successful validation refreshes immediately`() {
        assertTrue(
            PetCatalogRefreshPolicy.shouldRefresh(
                nowEpochMillis = NOW,
                lastValidatedAtEpochMillis = 0L,
                retryAfterEpochMillis = 0L
            )
        )
    }

    @Test
    fun `fresh catalog does not refresh before twenty four hours`() {
        assertFalse(
            PetCatalogRefreshPolicy.shouldRefresh(
                nowEpochMillis = NOW,
                lastValidatedAtEpochMillis = NOW -
                    PetCatalogRefreshPolicy.REFRESH_INTERVAL_MILLIS + 1L,
                retryAfterEpochMillis = 0L
            )
        )
        assertTrue(
            PetCatalogRefreshPolicy.shouldRefresh(
                nowEpochMillis = NOW,
                lastValidatedAtEpochMillis = NOW -
                    PetCatalogRefreshPolicy.REFRESH_INTERVAL_MILLIS,
                retryAfterEpochMillis = 0L
            )
        )
    }

    @Test
    fun `rate limit retry time blocks even a stale catalog`() {
        assertFalse(
            PetCatalogRefreshPolicy.shouldRefresh(
                nowEpochMillis = NOW,
                lastValidatedAtEpochMillis = 1L,
                retryAfterEpochMillis = NOW + 1L
            )
        )
        assertTrue(
            PetCatalogRefreshPolicy.shouldRefresh(
                nowEpochMillis = NOW,
                lastValidatedAtEpochMillis = 1L,
                retryAfterEpochMillis = NOW
            )
        )
    }

    @Test
    fun `rate limit headers choose the later server retry time`() {
        val retryAt = PetCatalogRefreshPolicy.rateLimitRetryAt(
            nowEpochMillis = NOW,
            retryAfterSeconds = "120",
            rateLimitResetEpochSeconds = ((NOW / 1_000L) + 300L).toString()
        )

        assertEquals(NOW + 300_000L, retryAt)
    }

    @Test
    fun `missing rate limit headers use one hour delay`() {
        val retryAt = PetCatalogRefreshPolicy.rateLimitRetryAt(
            nowEpochMillis = NOW,
            retryAfterSeconds = null,
            rateLimitResetEpochSeconds = null
        )

        assertEquals(NOW + 60L * 60L * 1_000L, retryAt)
    }

    @Test
    fun `untrusted rate limit header cannot suppress refresh beyond one day`() {
        val retryAt = PetCatalogRefreshPolicy.rateLimitRetryAt(
            nowEpochMillis = NOW,
            retryAfterSeconds = Long.MAX_VALUE.toString(),
            rateLimitResetEpochSeconds = null
        )

        assertEquals(NOW + 24L * 60L * 60L * 1_000L, retryAt)
    }

    private companion object {
        const val NOW = 2_000_000_000_000L
    }
}
