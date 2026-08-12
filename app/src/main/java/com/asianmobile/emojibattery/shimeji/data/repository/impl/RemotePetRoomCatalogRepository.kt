package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.data.model.PetRoomCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.PetRoomCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.PetRoomEntry
import com.asianmobile.emojibattery.shimeji.data.remote.GithubRoomCatalogClient
import com.asianmobile.emojibattery.shimeji.data.remote.RoomCatalogFetchResult
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomCatalogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cache-first room catalog: the cached document renders immediately and the network is only
 * consulted when the refresh policy allows it. Release builds refuse a catalog that has not
 * been approved for distribution, matching the pet and Battery catalogs.
 */
@Singleton
class RemotePetRoomCatalogRepository @Inject constructor(
    private val client: GithubRoomCatalogClient,
    private val parser: RoomCatalogParser
) : PetRoomCatalogRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow(PetRoomCatalogSnapshot())
    override val snapshot: StateFlow<PetRoomCatalogSnapshot> = state.asStateFlow()

    private var assetsByPath: Map<String, RoomCatalogAssetRecord> = emptyMap()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val cached = client.readCachedCatalog()
        val cachedDocument = cached?.json?.let { json ->
            runCatching { parser.parse(json) }.getOrNull()
        }
        if (cachedDocument != null) {
            publish(cachedDocument)
        }

        val metadata = cached?.metadata
        if (cachedDocument != null && metadata != null && !client.shouldRefreshCatalog(metadata)) {
            return@withContext
        }

        when (val result = client.fetchCatalog(metadata?.etag)) {
            is RoomCatalogFetchResult.Updated -> {
                val document = runCatching { parser.parse(result.json) }.getOrNull()
                if (document == null) {
                    failIfEmpty(PetRoomCatalogError.CATALOG_INVALID)
                    return@withContext
                }
                client.cacheCatalog(result.json, result.etag)
                publish(document)
            }

            is RoomCatalogFetchResult.NotModified -> {
                client.markCatalogNotModified(result.etag)
                if (cachedDocument == null) failIfEmpty(PetRoomCatalogError.CATALOG_INVALID)
            }

            is RoomCatalogFetchResult.RateLimited -> {
                client.deferCatalogRefresh(result.retryAtEpochMillis)
                if (cachedDocument == null) failIfEmpty(PetRoomCatalogError.CATALOG_UNAVAILABLE)
            }

            RoomCatalogFetchResult.Failed ->
                if (cachedDocument == null) failIfEmpty(PetRoomCatalogError.CATALOG_UNAVAILABLE)
        }
    }

    override suspend fun materializeAsset(path: String?): String? = withContext(Dispatchers.IO) {
        val record = path?.let(assetsByPath::get) ?: return@withContext null
        client.materializeAsset(
            relativePath = record.path,
            expectedSizeBytes = record.sizeBytes,
            expectedSha256 = record.sha256
        )?.absolutePath
    }

    override fun cachedAssetPath(path: String?): String? {
        val record = path?.let(assetsByPath::get) ?: return null
        return client.cachedAsset(
            relativePath = record.path,
            expectedSizeBytes = record.sizeBytes,
            expectedSha256 = record.sha256
        )?.absolutePath
    }

    private fun publish(document: RoomCatalogDocument) {
        if (!BuildConfig.DEBUG && !document.distributionApproved) {
            state.value = PetRoomCatalogSnapshot(
                isLoading = false,
                error = PetRoomCatalogError.DISTRIBUTION_NOT_APPROVED
            )
            assetsByPath = emptyMap()
            return
        }
        assetsByPath = document.rooms.flatMap { room ->
            listOf(room.background, room.thumbnail)
        }.associateBy(RoomCatalogAssetRecord::path)
        state.value = PetRoomCatalogSnapshot(
            rooms = document.rooms.map { room ->
                PetRoomEntry(
                    id = room.id,
                    name = room.name,
                    slug = room.slug,
                    entitlement = room.entitlement,
                    backgroundPath = room.background.path,
                    thumbnailPath = room.thumbnail.path
                )
            },
            defaultRoomId = document.defaultRoomId,
            catalogVersion = document.catalogVersion,
            isLoading = false,
            error = null
        )
    }

    private fun failIfEmpty(error: PetRoomCatalogError) {
        if (state.value.rooms.isNotEmpty()) return
        state.value = PetRoomCatalogSnapshot(isLoading = false, error = error)
    }
}
