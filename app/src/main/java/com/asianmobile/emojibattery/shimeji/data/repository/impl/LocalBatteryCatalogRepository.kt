package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
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
import kotlinx.coroutines.withContext

@Singleton
class LocalBatteryCatalogRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: BatteryCatalogParser
) : BatteryCatalogRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _snapshot = MutableStateFlow(initialSnapshot())
    override val snapshot: StateFlow<BatteryCatalogSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val root = localRoot()
        var failure = if (root == null) {
            BatteryCatalogError.LOCAL_STORAGE_UNAVAILABLE
        } else {
            BatteryCatalogError.LOCAL_CATALOG_MISSING
        }

        val catalogFile = root?.let { File(it, CATALOG_FILE) }
        if (catalogFile?.isFile == true) {
            try {
                val document = parser.parse(catalogFile.readText(Charsets.UTF_8))
                if (isDistributionAllowed(document) && isCurrentSchema(document)) {
                    val published = publish(
                        document = document,
                        rootPath = root.absolutePath,
                        requireComplete = BuildConfig.DEBUG,
                        resolve = { record -> resolveFileAsset(root, record) }
                    )
                    if (published) return@withContext
                }
                failure = if (!isDistributionAllowed(document)) {
                    BatteryCatalogError.DISTRIBUTION_NOT_APPROVED
                } else {
                    BatteryCatalogError.LOCAL_CATALOG_INVALID
                }
            } catch (error: BatteryCatalogParseException) {
                Log.w(TAG, "External battery catalog is invalid", error)
                failure = BatteryCatalogError.LOCAL_CATALOG_INVALID
            } catch (error: IOException) {
                Log.w(TAG, "External battery catalog cannot be read", error)
                failure = BatteryCatalogError.LOCAL_CATALOG_INVALID
            }
        }

        if (BuildConfig.DEBUG) {
            try {
                val json = context.assets.open(PACKAGED_CATALOG_FILE)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val document = parser.parse(json)
                val published = publish(
                    document = document,
                    rootPath = PACKAGED_CATALOG_ROOT,
                    requireComplete = true,
                    resolve = ::resolvePackagedAsset
                )
                if (published) return@withContext
                failure = BatteryCatalogError.LOCAL_CATALOG_INVALID
            } catch (error: IOException) {
                Log.i(TAG, "No packaged debug battery catalog is available")
            } catch (error: BatteryCatalogParseException) {
                Log.w(TAG, "Packaged debug battery catalog is invalid", error)
                failure = BatteryCatalogError.LOCAL_CATALOG_INVALID
            }
        }

        _snapshot.value = fallbackSnapshot(
            rootPath = root?.absolutePath.orEmpty(),
            error = failure
        )
    }

    override fun findTheme(themeId: Int): BatteryThemeEntry? =
        _snapshot.value.themes.firstOrNull { it.id == themeId }

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
            resolve(record.asset)?.let { path ->
                BatteryDecorationEntry(
                    id = record.id,
                    name = record.name,
                    assetPath = path,
                    type = BatteryDecorationType.BACKGROUND
                )
            }
        }
        val emotions = document.emotions.mapNotNull { record ->
            resolve(record.asset)?.let { path ->
                BatteryDecorationEntry(
                    id = record.id,
                    name = record.name,
                    assetPath = path,
                    type = BatteryDecorationType.EMOTION
                )
            }
        }
        if (requireComplete &&
            (themes.any { !it.assetsReady } ||
                backgrounds.size != document.backgrounds.size ||
                emotions.size != document.emotions.size)
        ) {
            return false
        }
        _snapshot.value = BatteryCatalogSnapshot(
            categories = listOf(BUILT_IN_BATTERY_CATEGORY) + document.categories,
            themes = listOf(BUILT_IN_BATTERY_THEME) + themes,
            backgrounds = backgrounds,
            emotions = emotions,
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
            (document.backgrounds.isNotEmpty() && document.emotions.isNotEmpty())

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
            if (validation.sizeBytes == record.sizeBytes &&
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

    private fun File.sha256(): String {
        return inputStream().buffered().use { input -> input.calculateDigest() }.sha256
    }

    private fun InputStream.calculateDigest(): AssetValidation {
        val messageDigest = MessageDigest.getInstance("SHA-256")
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
        return BatteryCatalogSnapshot(localRootPath = root?.absolutePath.orEmpty())
    }

    private fun fallbackSnapshot(
        rootPath: String,
        error: BatteryCatalogError
    ): BatteryCatalogSnapshot = BatteryCatalogSnapshot(
        localRootPath = rootPath,
        isLoading = false,
        error = error
    )

    private fun localRoot(): File? = context.getExternalFilesDir(LOCAL_DIRECTORY)

    private companion object {
        const val TAG = "BatteryCatalog"
        const val LOCAL_DIRECTORY = "battery_catalog"
        const val CATALOG_FILE = "catalog.json"
        const val PACKAGED_CATALOG_ROOT = "battery_catalog"
        const val PACKAGED_CATALOG_FILE = "$PACKAGED_CATALOG_ROOT/catalog.json"
        const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"
    }
}

private data class AssetValidation(
    val sizeBytes: Long,
    val sha256: String
)
