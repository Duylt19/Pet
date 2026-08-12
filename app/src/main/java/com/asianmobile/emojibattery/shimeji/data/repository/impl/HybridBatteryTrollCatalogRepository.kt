package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.util.Log
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollDistributionPolicy
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollAssetRecord
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollCatalogDocument
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollCatalogFetchResult
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollCatalogParseException
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollCatalogParser
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollRecord
import com.asianmobile.emojibattery.shimeji.data.remote.GithubBatteryTrollCatalogClient
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryTrollCatalogRepository
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

/**
 * Cache-first Battery Troll catalog: the cached document renders immediately and the network is
 * only consulted when the refresh policy allows it. Release builds refuse a catalog that has not
 * been approved for distribution. Unlike the Battery catalog there is no packaged snapshot, so a
 * failure falls back to an empty snapshot carrying the error instead of bundled content.
 */
@Singleton
class HybridBatteryTrollCatalogRepository @Inject constructor(
    private val client: GithubBatteryTrollCatalogClient,
    private val parser: BatteryTrollCatalogParser
) : BatteryTrollCatalogRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val state = MutableStateFlow(BatteryTrollCatalogSnapshot())
    override val snapshot: StateFlow<BatteryTrollCatalogSnapshot> = state.asStateFlow()

    @Volatile
    private var assetsByPath: Map<String, BatteryTrollAssetRecord> = emptyMap()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val cached = client.readCachedCatalog()
            val cachedPublished = cached?.json?.let(::parseCatalog)?.let(::publish) == true

            val metadata = cached?.metadata ?: client.readCatalogCacheMetadata()
            if (cachedPublished && !client.shouldRefreshCatalog(metadata)) {
                return@withLock
            }

            when (val result = client.fetchCatalog(metadata.etag)) {
                is BatteryTrollCatalogFetchResult.Updated -> {
                    val document = parseCatalog(result.json)
                    if (document == null) {
                        failIfEmpty(BatteryTrollCatalogError.CATALOG_INVALID)
                        return@withLock
                    }
                    client.cacheCatalog(result.json, result.etag)
                    // A rejected distribution status already published its own error state.
                    publish(document)
                }

                is BatteryTrollCatalogFetchResult.NotModified -> {
                    client.markCatalogNotModified(result.etag)
                    if (!cachedPublished) failIfEmpty(BatteryTrollCatalogError.CATALOG_INVALID)
                }

                is BatteryTrollCatalogFetchResult.RateLimited -> {
                    client.deferCatalogRefresh(result.retryAtEpochMillis)
                    if (!cachedPublished) {
                        failIfEmpty(BatteryTrollCatalogError.CATALOG_UNAVAILABLE)
                    }
                }

                BatteryTrollCatalogFetchResult.Failed ->
                    if (!cachedPublished) {
                        failIfEmpty(BatteryTrollCatalogError.CATALOG_UNAVAILABLE)
                    }
            }
        }
    }

    override fun findTroll(trollId: Int): BatteryTrollEntry? = state.value.findTroll(trollId)

    override suspend fun materializeAsset(path: String?): String? = withContext(Dispatchers.IO) {
        val record = path?.let(assetsByPath::get) ?: return@withContext null
        client.materializeAsset(
            relativePath = record.path,
            expectedSizeBytes = record.sizeBytes,
            expectedSha256 = record.sha256
        )?.absolutePath
    }

    private fun parseCatalog(json: String): BatteryTrollCatalogDocument? = try {
        parser.parse(json)
    } catch (error: BatteryTrollCatalogParseException) {
        Log.w(TAG, "Battery Troll catalog is invalid", error)
        null
    }

    /** Returns true when [document] became the published snapshot. */
    private fun publish(document: BatteryTrollCatalogDocument): Boolean {
        if (
            !BatteryTrollDistributionPolicy.isDistributionAllowed(
                status = document.distributionStatus,
                isDebugBuild = BuildConfig.DEBUG
            )
        ) {
            assetsByPath = emptyMap()
            state.value = BatteryTrollCatalogSnapshot(
                distributionStatus = document.distributionStatus,
                isLoading = false,
                error = BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED
            )
            return false
        }
        assetsByPath = document.trolls
            .flatMap(BatteryTrollRecord::assets)
            .associateBy(BatteryTrollAssetRecord::path)
        state.value = BatteryTrollCatalogSnapshot(
            trolls = document.trolls.map { troll ->
                BatteryTrollEntry(
                    id = troll.id,
                    name = troll.name,
                    slug = troll.slug,
                    order = troll.order,
                    entitlement = troll.entitlement,
                    batteryOrientation = troll.batteryOrientation,
                    thumbnailPath = troll.thumbnail.path,
                    emojiPaths = troll.emoji.map(BatteryTrollAssetRecord::path),
                    batteryPaths = troll.battery.map(BatteryTrollAssetRecord::path)
                )
            },
            catalogVersion = document.catalogVersion,
            capturedAt = document.capturedAt,
            distributionStatus = document.distributionStatus,
            isLoading = false,
            error = null
        )
        return true
    }

    private fun failIfEmpty(error: BatteryTrollCatalogError) {
        val current = state.value
        if (
            current.trolls.isNotEmpty() ||
            current.error == BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED
        ) {
            return
        }
        state.value = BatteryTrollCatalogSnapshot(isLoading = false, error = error)
    }

    private companion object {
        const val TAG = "BatteryTrollCatalog"
    }
}
