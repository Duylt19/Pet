package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BATTERY_EMOTION_GROUPS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryEmotionGroup
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_BATTERY_BACKGROUND_ID
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryCatalogFetchResult
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryServerConfig
import com.asianmobile.emojibattery.shimeji.data.remote.GithubBatteryCatalogClient
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
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
class HybridBatteryCatalogRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: BatteryCatalogParser,
    private val remoteClient: GithubBatteryCatalogClient
) : BatteryCatalogRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val _snapshot = MutableStateFlow(initialSnapshot())
    override val snapshot: StateFlow<BatteryCatalogSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var remoteAssetRecords: Map<String, BatteryCatalogAssetRecord> = emptyMap()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            _snapshot.value = _snapshot.value.copy(isLoading = true, error = null)
            val cached = remoteClient.readCachedCatalog()
            val cachedDocument = cached?.json?.let(::parseAllowedRemote)
            var cachedPublished = false
            if (cachedDocument != null) {
                cachedPublished = publishRemote(cachedDocument)
            }

            val metadata = cached?.metadata ?: remoteClient.readCatalogCacheMetadata()
            if (remoteClient.shouldRefreshCatalog(metadata)) {
                when (val result = remoteClient.fetchCatalog(metadata.etag)) {
                    is BatteryCatalogFetchResult.Updated -> {
                        val document = parseAllowedRemote(result.json)
                        if (document != null && publishRemote(document)) {
                            remoteClient.cacheCatalog(result.json, result.etag)
                            return@withLock
                        }
                        if (!cachedPublished) {
                            refreshLocalFallback(BatteryCatalogError.REMOTE_CATALOG_INVALID)
                        }
                        return@withLock
                    }

                    is BatteryCatalogFetchResult.NotModified -> {
                        if (cachedPublished) {
                            remoteClient.markCatalogNotModified(result.etag)
                            return@withLock
                        }
                    }

                    is BatteryCatalogFetchResult.RateLimited -> {
                        remoteClient.deferCatalogRefresh(result.retryAtEpochMillis)
                        if (cachedPublished) return@withLock
                    }

                    BatteryCatalogFetchResult.Failed -> if (cachedPublished) return@withLock
                }
            } else if (cachedPublished) {
                return@withLock
            }

            refreshLocalFallback(BatteryCatalogError.REMOTE_CATALOG_UNAVAILABLE)
        }
    }

    override fun findTheme(themeId: Int): BatteryThemeEntry? =
        _snapshot.value.themes.firstOrNull { it.id == themeId }

    override suspend fun materializeAsset(path: String?): String? = withContext(Dispatchers.IO) {
        if (path == null) return@withContext null
        val record = remoteAssetRecords[path] ?: return@withContext path
        remoteClient.materializeAsset(
            url = path,
            relativePath = record.path,
            expectedSizeBytes = record.sizeBytes,
            expectedSha256 = record.sha256
        )?.absolutePath
    }

    private fun parseAllowedRemote(json: String): BatteryCatalogDocument? = try {
        parser.parse(json).takeIf(::isDistributionAllowed)
    } catch (error: BatteryCatalogParseException) {
        Log.w(TAG, "Remote Battery catalog is invalid", error)
        null
    }

    private fun publishRemote(document: BatteryCatalogDocument): Boolean {
        val records = mutableMapOf<String, BatteryCatalogAssetRecord>()
        val published = publish(
            document = document,
            rootPath = BatteryServerConfig.CATALOG_URL,
            requireComplete = true,
            resolve = { record ->
                BatteryServerConfig.resolveAsset(record.path).also { url ->
                    records[url] = record
                }
            }
        )
        if (published) remoteAssetRecords = records
        return published
    }

    private fun refreshLocalFallback(remoteFailure: BatteryCatalogError) {
        remoteAssetRecords = emptyMap()
        val root = localRoot()
        val catalogFile = root?.let { File(it, CATALOG_FILE) }
        if (catalogFile?.isFile == true) {
            try {
                val document = parser.parse(catalogFile.readText(Charsets.UTF_8))
                if (isDistributionAllowed(document) && isCurrentSchema(document)) {
                    if (
                        publish(
                            document = document,
                            rootPath = root.absolutePath,
                            requireComplete = BuildConfig.DEBUG,
                            resolve = { record -> resolveFileAsset(root, record) }
                        )
                    ) {
                        return
                    }
                }
            } catch (error: BatteryCatalogParseException) {
                Log.w(TAG, "External Battery catalog is invalid", error)
            } catch (error: IOException) {
                Log.w(TAG, "External Battery catalog cannot be read", error)
            }
        }

        if (BuildConfig.DEBUG) {
            try {
                val json = context.assets.open(PACKAGED_CATALOG_FILE)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val document = parser.parse(json)
                if (
                    publish(
                        document = document,
                        rootPath = PACKAGED_CATALOG_ROOT,
                        requireComplete = true,
                        resolve = ::resolvePackagedAsset
                    )
                ) {
                    return
                }
            } catch (error: IOException) {
                Log.i(TAG, "No packaged debug Battery catalog is available")
            } catch (error: BatteryCatalogParseException) {
                Log.w(TAG, "Packaged debug Battery catalog is invalid", error)
            }
        }

        _snapshot.value = fallbackSnapshot(
            rootPath = root?.absolutePath.orEmpty(),
            error = remoteFailure
        )
    }

    private fun publish(
        document: BatteryCatalogDocument,
        rootPath: String,
        requireComplete: Boolean,
        resolve: (BatteryCatalogAssetRecord) -> String?
    ): Boolean {
        val themes = document.themes.map { record ->
            val thumbnail = resolve(record.thumbnail)
            val battery = resolve(record.battery)
            val emoji = resolve(record.emoji)
            BatteryThemeEntry(
                id = record.id,
                name = record.name,
                categoryId = record.categoryId,
                categoryName = record.categoryName,
                entitlement = record.entitlement,
                thumbnailPath = thumbnail,
                batteryPath = battery,
                emojiPath = emoji,
                assetsReady = thumbnail != null && battery != null && emoji != null
            )
        }
        val backgrounds = document.backgrounds.mapNotNull { record ->
            if (record.id == DEFAULT_BATTERY_BACKGROUND_ID) {
                builtInDefaultBackground()
            } else {
                val assetPath = resolve(record.asset)
                val previewPath = resolve(record.preview ?: record.asset)
                if (assetPath != null && previewPath != null) {
                    BatteryDecorationEntry(
                        id = record.id,
                        name = record.name,
                        assetPath = assetPath,
                        previewPath = previewPath,
                        order = record.order,
                        type = BatteryDecorationType.BACKGROUND
                    )
                } else null
            }
        }.sortedWith(compareBy(BatteryDecorationEntry::order, BatteryDecorationEntry::id))
        val emotions = document.emotions.mapNotNull { record ->
            val assetPath = resolve(record.asset)
            val previewPath = resolve(record.preview ?: record.asset)
            if (assetPath != null && previewPath != null) {
                BatteryDecorationEntry(
                    id = record.id,
                    name = record.name,
                    assetPath = assetPath,
                    previewPath = previewPath,
                    groupKey = record.groupKey,
                    order = record.order,
                    type = BatteryDecorationType.EMOTION
                )
            } else null
        }
        val emotionGroups = document.emotionGroups.mapNotNull { record ->
            val backgroundPath = record.background?.let(resolve)
            if (record.background == null || backgroundPath != null) {
                BatteryEmotionGroup(
                    key = record.key,
                    emotionIds = record.emotionIds,
                    backgroundPath = backgroundPath
                )
            } else null
        }
        val animations = document.animations.mapNotNull { record ->
            resolve(record.asset)?.let { path ->
                BatteryAnimationEntry(
                    id = record.id,
                    name = record.name,
                    assetPath = path,
                    type = record.type
                )
            }
        }
        if (
            requireComplete &&
            (
                themes.any { !it.assetsReady } ||
                    backgrounds.size != document.backgrounds.size ||
                    emotions.size != document.emotions.size ||
                    emotionGroups.size != document.emotionGroups.size ||
                    animations.size != document.animations.size
                )
        ) {
            return false
        }
        _snapshot.value = BatteryCatalogSnapshot(
            categories = listOf(BUILT_IN_BATTERY_CATEGORY) + document.categories,
            themes = listOf(BUILT_IN_BATTERY_THEME) + themes,
            backgrounds = backgrounds,
            emotions = emotions,
            emotionGroups = emotionGroups.ifEmpty { BATTERY_EMOTION_GROUPS },
            animations = animations,
            catalogVersion = document.catalogVersion,
            capturedAt = document.capturedAt,
            distributionStatus = document.distributionStatus,
            localRootPath = rootPath,
            isLoading = false
        )
        return true
    }

    private fun isDistributionAllowed(document: BatteryCatalogDocument): Boolean =
        BuildConfig.DEBUG ||
            document.distributionStatus == BatteryCatalogDistributionStatus.APPROVED

    private fun isCurrentSchema(document: BatteryCatalogDocument): Boolean =
        !BuildConfig.DEBUG ||
            (
                document.backgrounds.size == EXPECTED_BACKGROUND_COUNT &&
                    document.backgrounds.take(FIGMA_BACKGROUND_COUNT).all {
                        it.preview != null
                    } &&
                    document.emotions.isNotEmpty() &&
                    document.animations.isNotEmpty()
                )

    private fun resolveFileAsset(
        root: File,
        record: BatteryCatalogAssetRecord
    ): String? {
        val target = File(root, record.path)
        val canonicalRoot = root.canonicalFile
        val canonicalTarget = target.canonicalFile
        if (!canonicalTarget.path.startsWith(canonicalRoot.path + File.separator)) return null
        return canonicalTarget.takeIf {
            it.isFile &&
                it.length() == record.sizeBytes &&
                it.sha256() == record.sha256
        }?.absolutePath
    }

    private fun resolvePackagedAsset(record: BatteryCatalogAssetRecord): String? {
        val assetPath = "$PACKAGED_CATALOG_ROOT/${record.path}"
        return try {
            val validation = context.assets.open(assetPath, AssetManager.ACCESS_STREAMING)
                .use { input -> input.calculateDigest() }
            if (
                validation.sizeBytes == record.sizeBytes &&
                validation.sha256 == record.sha256
            ) {
                "$ANDROID_ASSET_URI_PREFIX$assetPath"
            } else {
                null
            }
        } catch (error: IOException) {
            null
        }
    }

    private fun File.sha256(): String =
        inputStream().buffered().use { input -> input.calculateDigest() }.sha256

    private fun InputStream.calculateDigest(): AssetValidation {
        val messageDigest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var sizeBytes = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            messageDigest.update(buffer, 0, read)
            sizeBytes += read
        }
        val sha256 = messageDigest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
        return AssetValidation(sizeBytes = sizeBytes, sha256 = sha256)
    }

    private fun initialSnapshot(): BatteryCatalogSnapshot {
        val root = localRoot()
        return BatteryCatalogSnapshot(
            backgrounds = listOf(builtInDefaultBackground()),
            localRootPath = root?.absolutePath.orEmpty()
        )
    }

    private fun fallbackSnapshot(
        rootPath: String,
        error: BatteryCatalogError
    ): BatteryCatalogSnapshot = BatteryCatalogSnapshot(
        backgrounds = listOf(builtInDefaultBackground()),
        localRootPath = rootPath,
        isLoading = false,
        error = error
    )

    private fun builtInDefaultBackground(): BatteryDecorationEntry {
        val uri = buildString {
            append("android.resource://")
            append(context.packageName)
            append('/')
            append(R.drawable.img_battery_background_default)
        }
        return BatteryDecorationEntry(
            id = DEFAULT_BATTERY_BACKGROUND_ID,
            name = "status_background_01",
            assetPath = uri,
            previewPath = uri,
            order = 0,
            type = BatteryDecorationType.BACKGROUND
        )
    }

    private fun localRoot(): File? = context.getExternalFilesDir(LOCAL_DIRECTORY)

    private companion object {
        const val TAG = "BatteryCatalog"
        const val LOCAL_DIRECTORY = "battery_catalog"
        const val CATALOG_FILE = "catalog.json"
        const val PACKAGED_CATALOG_ROOT = "battery_catalog"
        const val PACKAGED_CATALOG_FILE = "$PACKAGED_CATALOG_ROOT/catalog.json"
        const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"
        const val EXPECTED_BACKGROUND_COUNT = 38
        const val FIGMA_BACKGROUND_COUNT = 18
        const val SHA_256 = "SHA-256"
    }
}

private data class AssetValidation(
    val sizeBytes: Long,
    val sha256: String
)
