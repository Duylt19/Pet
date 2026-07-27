package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.LegacyShimejiPackInstaller
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInstallResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
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
class LocalOwnerPetCatalogRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: OwnerPetCatalogParser,
    private val installer: LegacyShimejiPackInstaller
) : OwnerPetCatalogRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _snapshot = MutableStateFlow(initialSnapshot())
    override val snapshot: StateFlow<OwnerPetCatalogSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val root = localRoot()
        if (root == null) {
            _snapshot.value = OwnerPetCatalogSnapshot(
                isLoading = false,
                error = OwnerPetCatalogError.LOCAL_STORAGE_UNAVAILABLE
            )
            return@withContext
        }
        val catalogFile = File(root, CATALOG_FILE)
        if (!catalogFile.isFile) {
            _snapshot.value = OwnerPetCatalogSnapshot(
                localRootPath = root.absolutePath,
                isLoading = false,
                error = OwnerPetCatalogError.LOCAL_CATALOG_MISSING
            )
            return@withContext
        }
        try {
            val records = parser.parse(catalogFile.readText(Charsets.UTF_8))
            val entries = records.map { record ->
                val thumbnail = File(root, "$THUMBNAIL_DIRECTORY/${record.id}.png")
                val archive = File(root, "$ARCHIVE_DIRECTORY/${record.id}.zip")
                OwnerPetCatalogEntry(
                    id = record.id,
                    name = record.name,
                    category = record.category,
                    author = record.author,
                    thumbnailPath = thumbnail.takeIf(File::isFile)?.absolutePath,
                    hasLocalArchive = archive.isFile
                )
            }
            _snapshot.value = OwnerPetCatalogSnapshot(
                entries = entries,
                localRootPath = root.absolutePath,
                isLoading = false
            )
        } catch (error: OwnerPetCatalogParseException) {
            _snapshot.value = OwnerPetCatalogSnapshot(
                localRootPath = root.absolutePath,
                isLoading = false,
                error = OwnerPetCatalogError.LOCAL_CATALOG_INVALID
            )
        } catch (error: IOException) {
            _snapshot.value = OwnerPetCatalogSnapshot(
                localRootPath = root.absolutePath,
                isLoading = false,
                error = OwnerPetCatalogError.LOCAL_CATALOG_INVALID
            )
        }
    }

    override suspend fun preparePack(petId: Int): PetPackInstallResult =
        withContext(Dispatchers.IO) {
            val entry = _snapshot.value.entries.firstOrNull { it.id == petId }
                ?: return@withContext PetPackInstallResult.Failed("Pet is not in the local catalog")
            val root = localRoot()
                ?: return@withContext PetPackInstallResult.Failed("Local storage is unavailable")
            val archive = File(root, "$ARCHIVE_DIRECTORY/$petId.zip")
            if (!archive.isFile) {
                return@withContext PetPackInstallResult.Failed("Local pet archive is missing")
            }
            installer.install(entry, archive)
        }

    private fun initialSnapshot(): OwnerPetCatalogSnapshot {
        val root = localRoot()
        return OwnerPetCatalogSnapshot(localRootPath = root?.absolutePath.orEmpty())
    }

    private fun localRoot(): File? = context.getExternalFilesDir(LOCAL_DIRECTORY)

    private companion object {
        const val LOCAL_DIRECTORY = "pet_catalog"
        const val CATALOG_FILE = "shimeji.json"
        const val ARCHIVE_DIRECTORY = "data"
        const val THUMBNAIL_DIRECTORY = "thumb"
    }
}
