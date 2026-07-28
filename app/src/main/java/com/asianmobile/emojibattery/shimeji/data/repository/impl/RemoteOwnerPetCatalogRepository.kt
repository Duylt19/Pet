package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.remote.GithubPetCatalogClient
import com.asianmobile.emojibattery.shimeji.data.remote.PetCatalogFetchResult
import com.asianmobile.emojibattery.shimeji.data.remote.PetServerConfig
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.LegacyShimejiPackInstaller
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstallResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class RemoteOwnerPetCatalogRepository @Inject constructor(
    private val parser: OwnerPetCatalogParser,
    private val client: GithubPetCatalogClient,
    private val installer: LegacyShimejiPackInstaller
) : OwnerPetCatalogRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val _snapshot = MutableStateFlow(OwnerPetCatalogSnapshot())
    override val snapshot: StateFlow<OwnerPetCatalogSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            _snapshot.value = _snapshot.value.copy(isLoading = true, error = null)

            val cached = client.readCachedCatalog()
            val cachedDocument = cached?.json?.let { json ->
                runCatching { parser.parseDocument(json) }.getOrNull()
            }
            val metadata = cached?.metadata ?: client.readCatalogCacheMetadata()
            if (cachedDocument != null) {
                _snapshot.value = cachedDocument.toSnapshot()
            }
            if (!client.shouldRefreshCatalog(metadata)) {
                if (cachedDocument == null) {
                    _snapshot.value = errorSnapshot(
                        OwnerPetCatalogError.REMOTE_CATALOG_UNAVAILABLE
                    )
                }
                return@withLock
            }

            when (val result = client.fetchCatalog(metadata.etag)) {
                is PetCatalogFetchResult.Updated -> {
                    val remoteDocument = runCatching {
                        parser.parseDocument(result.json)
                    }.getOrNull()
                    if (remoteDocument != null) {
                        client.cacheCatalog(result.json, result.etag)
                        _snapshot.value = remoteDocument.toSnapshot()
                    } else if (cachedDocument == null) {
                        _snapshot.value = errorSnapshot(OwnerPetCatalogError.REMOTE_CATALOG_INVALID)
                    }
                }

                is PetCatalogFetchResult.NotModified -> {
                    if (cachedDocument != null) {
                        client.markCatalogNotModified(result.etag)
                        _snapshot.value = cachedDocument.toSnapshot()
                    } else {
                        _snapshot.value = errorSnapshot(OwnerPetCatalogError.REMOTE_CATALOG_INVALID)
                    }
                }

                is PetCatalogFetchResult.RateLimited -> {
                    client.deferCatalogRefresh(result.retryAtEpochMillis)
                    if (cachedDocument == null) {
                        _snapshot.value = errorSnapshot(
                            OwnerPetCatalogError.REMOTE_CATALOG_UNAVAILABLE
                        )
                    }
                }

                PetCatalogFetchResult.Failed -> {
                    if (cachedDocument == null) {
                        _snapshot.value = errorSnapshot(
                            OwnerPetCatalogError.REMOTE_CATALOG_UNAVAILABLE
                        )
                    }
                }
            }
        }
    }

    private fun errorSnapshot(error: OwnerPetCatalogError): OwnerPetCatalogSnapshot =
        OwnerPetCatalogSnapshot(
            isLoading = false,
            error = error
        )

    override suspend fun preparePack(petId: Int): PetPackInstallResult =
        withContext(Dispatchers.IO) {
            val entry = _snapshot.value.entries.firstOrNull { it.id == petId }
                ?: return@withContext PetPackInstallResult.Failed(
                    "Pet is not in the server catalog"
                )
            val archiveUrl = entry.archiveUrl
                ?: return@withContext PetPackInstallResult.Failed("Pet archive is unavailable")
            val archiveSize = entry.archiveSizeBytes
                ?: return@withContext PetPackInstallResult.Failed("Pet archive size is missing")
            val archiveSha256 = entry.archiveSha256
                ?: return@withContext PetPackInstallResult.Failed("Pet archive hash is missing")
            val archive = client.downloadArchive(
                petId = entry.id,
                url = archiveUrl,
                expectedSizeBytes = archiveSize,
                expectedSha256 = archiveSha256
            ) ?: return@withContext PetPackInstallResult.Failed(
                "Unable to download or verify this pet"
            )
            installer.install(entry, archive)
        }

    private fun OwnerPetCatalogDocument.toSnapshot(): OwnerPetCatalogSnapshot =
        OwnerPetCatalogSnapshot(
            entries = records.map { record ->
                OwnerPetCatalogEntry(
                    id = record.id,
                    name = record.name,
                    category = record.category,
                    author = record.author,
                    thumbnailPath = record.thumbnail?.let { PetServerConfig.resolve(it.path) },
                    hasLocalArchive = record.archive != null,
                    archiveUrl = record.archive?.let { PetServerConfig.resolve(it.path) },
                    archiveSizeBytes = record.archive?.sizeBytes,
                    archiveSha256 = record.archive?.sha256
                )
            },
            catalogVersion = catalogVersion,
            isLoading = false
        )
}
