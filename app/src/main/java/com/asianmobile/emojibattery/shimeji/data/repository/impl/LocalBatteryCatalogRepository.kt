package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
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
        if (root == null) {
            _snapshot.value = fallbackSnapshot(
                root = null,
                error = BatteryCatalogError.LOCAL_STORAGE_UNAVAILABLE
            )
            return@withContext
        }
        val catalogFile = File(root, CATALOG_FILE)
        if (!catalogFile.isFile) {
            _snapshot.value = fallbackSnapshot(
                root = root,
                error = BatteryCatalogError.LOCAL_CATALOG_MISSING
            )
            return@withContext
        }
        try {
            val document = parser.parse(catalogFile.readText(Charsets.UTF_8))
            if (!BuildConfig.DEBUG &&
                document.distributionStatus != BatteryCatalogDistributionStatus.APPROVED
            ) {
                _snapshot.value = fallbackSnapshot(
                    root = root,
                    error = BatteryCatalogError.DISTRIBUTION_NOT_APPROVED
                )
                return@withContext
            }
            val themes = document.themes.map { record ->
                val thumbnail = resolveAsset(root, record.thumbnail)
                val battery = resolveAsset(root, record.battery)
                val emoji = resolveAsset(root, record.emoji)
                BatteryThemeEntry(
                    id = record.id,
                    name = record.name,
                    categoryId = record.categoryId,
                    categoryName = record.categoryName,
                    entitlement = record.entitlement,
                    thumbnailPath = thumbnail?.absolutePath,
                    batteryPath = battery?.absolutePath,
                    emojiPath = emoji?.absolutePath,
                    assetsReady = thumbnail != null && battery != null && emoji != null
                )
            }
            _snapshot.value = BatteryCatalogSnapshot(
                categories = listOf(BUILT_IN_BATTERY_CATEGORY) + document.categories,
                themes = listOf(BUILT_IN_BATTERY_THEME) + themes,
                catalogVersion = document.catalogVersion,
                capturedAt = document.capturedAt,
                distributionStatus = document.distributionStatus,
                localRootPath = root.absolutePath,
                isLoading = false
            )
        } catch (error: BatteryCatalogParseException) {
            _snapshot.value = fallbackSnapshot(
                root = root,
                error = BatteryCatalogError.LOCAL_CATALOG_INVALID
            )
        } catch (error: IOException) {
            _snapshot.value = fallbackSnapshot(
                root = root,
                error = BatteryCatalogError.LOCAL_CATALOG_INVALID
            )
        }
    }

    override fun findTheme(themeId: Int): BatteryThemeEntry? =
        _snapshot.value.themes.firstOrNull { it.id == themeId }

    private fun resolveAsset(
        root: File,
        record: BatteryCatalogAssetRecord
    ): File? {
        val target = File(root, record.path)
        val canonicalRoot = root.canonicalFile
        val canonicalTarget = target.canonicalFile
        if (!canonicalTarget.path.startsWith(canonicalRoot.path + File.separator)) return null
        return canonicalTarget.takeIf {
            it.isFile &&
                it.length() == record.sizeBytes &&
                it.sha256() == record.sha256
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun initialSnapshot(): BatteryCatalogSnapshot {
        val root = localRoot()
        return BatteryCatalogSnapshot(localRootPath = root?.absolutePath.orEmpty())
    }

    private fun fallbackSnapshot(
        root: File?,
        error: BatteryCatalogError
    ): BatteryCatalogSnapshot = BatteryCatalogSnapshot(
        localRootPath = root?.absolutePath.orEmpty(),
        isLoading = false,
        error = error
    )

    private fun localRoot(): File? = context.getExternalFilesDir(LOCAL_DIRECTORY)

    private companion object {
        const val LOCAL_DIRECTORY = "battery_catalog"
        const val CATALOG_FILE = "catalog.json"
    }
}
