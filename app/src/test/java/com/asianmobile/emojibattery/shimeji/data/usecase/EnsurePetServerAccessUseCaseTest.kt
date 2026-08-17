package com.asianmobile.emojibattery.shimeji.data.usecase

import com.asianmobile.emojibattery.shimeji.data.repository.RemoteConfigRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class EnsurePetServerAccessUseCaseTest {
    @Test
    fun `available token skips remote config refresh`() = runBlocking {
        val repository = FakeRemoteConfigRepository(hasToken = true)

        val result = EnsurePetServerAccessUseCase(repository)()

        assertEquals(true, result)
        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun `missing token refreshes remote config before granting server access`() = runBlocking {
        val repository = FakeRemoteConfigRepository(
            hasToken = false,
            tokenAfterRefresh = true,
        )

        val result = EnsurePetServerAccessUseCase(repository)()

        assertEquals(true, result)
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `missing token remains unavailable when remote config cannot recover it`() = runBlocking {
        val repository = FakeRemoteConfigRepository(
            hasToken = false,
            refreshSucceeds = false,
        )

        val result = EnsurePetServerAccessUseCase(repository)()

        assertEquals(false, result)
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `successful fetch without token remains unavailable`() = runBlocking {
        val repository = FakeRemoteConfigRepository(
            hasToken = false,
            refreshSucceeds = true,
            tokenAfterRefresh = false,
        )

        val result = EnsurePetServerAccessUseCase(repository)()

        assertEquals(false, result)
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun `concurrent requests share one remote config refresh`() = runBlocking {
        val repository = FakeRemoteConfigRepository(
            hasToken = false,
            tokenAfterRefresh = true,
        )
        val useCase = EnsurePetServerAccessUseCase(repository)

        val results = listOf(
            async { useCase() },
            async { useCase() },
        ).awaitAll()

        assertEquals(listOf(true, true), results)
        assertEquals(1, repository.refreshCount)
    }

    private class FakeRemoteConfigRepository(
        private var hasToken: Boolean,
        private val refreshSucceeds: Boolean = true,
        private val tokenAfterRefresh: Boolean = false,
    ) : RemoteConfigRepository {
        var refreshCount: Int = 0
            private set

        override fun hasPetServerToken(): Boolean = hasToken

        override suspend fun refresh(): Boolean {
            refreshCount += 1
            yield()
            if (refreshSucceeds) hasToken = tokenAfterRefresh
            return refreshSucceeds
        }
    }
}
