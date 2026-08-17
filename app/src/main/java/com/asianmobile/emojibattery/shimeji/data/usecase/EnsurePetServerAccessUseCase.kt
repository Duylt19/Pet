package com.asianmobile.emojibattery.shimeji.data.usecase

import com.asianmobile.emojibattery.shimeji.data.repository.RemoteConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class EnsurePetServerAccessUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    private val refreshMutex = Mutex()

    suspend operator fun invoke(): Boolean {
        if (remoteConfigRepository.hasPetServerToken()) return true
        return refreshMutex.withLock {
            if (remoteConfigRepository.hasPetServerToken()) return@withLock true
            remoteConfigRepository.refresh() &&
                remoteConfigRepository.hasPetServerToken()
        }
    }
}
