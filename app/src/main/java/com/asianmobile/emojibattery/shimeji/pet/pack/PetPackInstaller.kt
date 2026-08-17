package com.asianmobile.emojibattery.shimeji.pet.pack

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

sealed interface PetPackInstallResult {
    data class Installed(val pack: PetPack) : PetPackInstallResult
    data class Rejected(val reason: String) : PetPackInstallResult
    data class Failed(val reason: String) : PetPackInstallResult
}

class PetPackInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val extractor: PetPackArchiveExtractor,
    private val diskLoader: PetPackDiskLoader
) {
    suspend fun install(uri: Uri): PetPackInstallResult = withContext(Dispatchers.IO) {
        val root = packStorageRoot(context)
        cleanupStaleStaging(root)
        val staging = File(root, "$STAGING_DIRECTORY/${UUID.randomUUID()}")
        val archive = File(staging, ARCHIVE_FILE)
        val unpacked = File(staging, UNPACKED_DIRECTORY)
        try {
            if (!staging.mkdirs()) throw PetPackInstallException("Unable to create staging area")
            copyArchive(uri, archive)
            extractor.extract(archive, unpacked)
            val stagedPack = diskLoader.load(unpacked)
            val destination = installedDirectory(
                root = root,
                id = stagedPack.manifest.id,
                version = stagedPack.manifest.version
            )
            if (destination.exists()) {
                val existing = diskLoader.load(destination)
                return@withContext PetPackInstallResult.Installed(existing)
            }
            val parent = destination.parentFile
                ?: throw PetPackInstallException("Invalid install destination")
            if (!parent.mkdirs() && !parent.isDirectory) {
                throw PetPackInstallException("Unable to create install destination")
            }
            if (!unpacked.renameTo(destination)) {
                throw PetPackInstallException("Unable to atomically promote pet pack")
            }
            PetPackInstallResult.Installed(diskLoader.load(destination))
        } catch (error: PetPackInstallException) {
            PetPackInstallResult.Rejected(error.message ?: "Pet pack was rejected")
        } catch (error: IOException) {
            PetPackInstallResult.Rejected("Archive is malformed or incomplete")
        } catch (error: SecurityException) {
            PetPackInstallResult.Failed("The selected file cannot be read")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            PetPackInstallResult.Failed("Unable to install this pet pack")
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun copyArchive(uri: Uri, destination: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw PetPackInstallException("The selected file cannot be opened")
        input.use { source ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                var totalBytes = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    totalBytes += count
                    if (totalBytes > PetPackArchiveExtractor.MAX_ARCHIVE_BYTES) {
                        throw PetPackInstallException("Archive exceeds the size limit")
                    }
                    output.write(buffer, 0, count)
                }
                if (totalBytes == 0L) throw PetPackInstallException("Archive is empty")
            }
        }
    }

    private fun cleanupStaleStaging(root: File) {
        val cutoff = System.currentTimeMillis() - STAGING_MAX_AGE_MILLIS
        File(root, STAGING_DIRECTORY).listFiles().orEmpty()
            .filter { it.lastModified() < cutoff }
            .forEach(File::deleteRecursively)
    }

    companion object {
        private const val STAGING_DIRECTORY = ".staging"
        private const val ARCHIVE_FILE = "pack.zip"
        private const val UNPACKED_DIRECTORY = "unpacked"
        private const val COPY_BUFFER_BYTES = 16 * 1024
        private const val STAGING_MAX_AGE_MILLIS = 24L * 60L * 60L * 1_000L

        fun packStorageRoot(context: Context): File = File(context.filesDir, "pet_packs")

        fun installedDirectory(root: File, id: String, version: Int): File =
            File(root, "installed/$id/$version")
    }
}
