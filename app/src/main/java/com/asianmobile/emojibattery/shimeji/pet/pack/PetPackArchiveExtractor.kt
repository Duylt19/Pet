package com.asianmobile.emojibattery.shimeji.pet.pack

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class PetPackArchiveExtractor {
    fun extract(archive: File, destination: File) {
        check(destination.mkdirs() || destination.isDirectory) {
            "Unable to create extraction directory"
        }
        val destinationPrefix = destination.canonicalPath + File.separator
        val seenEntries = mutableSetOf<String>()
        var entryCount = 0
        var totalBytes = 0L

        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_ARCHIVE_ENTRIES) {
                    throw PetPackInstallException("Archive contains too many entries")
                }
                val normalizedName = normalizeEntryName(entry.name, entry.isDirectory)
                if (!seenEntries.add(normalizedName)) {
                    throw PetPackInstallException("Archive contains duplicate entries")
                }
                val target = File(destination, normalizedName)
                if (!target.canonicalPath.startsWith(destinationPrefix)) {
                    throw PetPackInstallException("Archive entry escapes the pack directory")
                }
                if (entry.isDirectory) {
                    if (!target.mkdirs() && !target.isDirectory) {
                        throw PetPackInstallException("Unable to create archive directory")
                    }
                    input.closeEntry()
                    continue
                }
                target.parentFile?.let { parent ->
                    if (!parent.mkdirs() && !parent.isDirectory) {
                        throw PetPackInstallException("Unable to create asset directory")
                    }
                }
                var entryBytes = 0L
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        if (entryBytes > MAX_SINGLE_ENTRY_BYTES || totalBytes > MAX_UNPACKED_BYTES) {
                            throw PetPackInstallException("Archive exceeds the unpacked size limit")
                        }
                        output.write(buffer, 0, count)
                    }
                }
                input.closeEntry()
            }
        }
        if (entryCount == 0) throw PetPackInstallException("Archive is empty")
        val ratioLimit = (archive.length().coerceAtLeast(MIN_RATIO_BASE_BYTES) * MAX_EXPANSION_RATIO)
            .coerceAtMost(MAX_UNPACKED_BYTES)
        if (totalBytes > ratioLimit) {
            throw PetPackInstallException("Archive expansion ratio is too high")
        }
    }

    private fun normalizeEntryName(rawName: String, isDirectory: Boolean): String {
        val name = rawName.removeSuffix("/")
        if (name.isBlank() || name.length > MAX_ENTRY_PATH_LENGTH ||
            name.startsWith('/') || name.startsWith('\\') || '\\' in name || '\u0000' in name
        ) {
            throw PetPackInstallException("Archive contains an unsafe path")
        }
        val segments = name.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) {
            throw PetPackInstallException("Archive contains an unsafe path")
        }
        if (!isDirectory) {
            val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            val isManifest = name == PET_PACK_MANIFEST_FILE
            if (!isManifest && extension !in PetPackValidator.ALLOWED_IMAGE_EXTENSIONS) {
                throw PetPackInstallException("Archive contains an unsupported file type")
            }
        }
        return name
    }

    companion object {
        const val MAX_ARCHIVE_BYTES = 20L * 1024L * 1024L
        const val MAX_UNPACKED_BYTES = 32L * 1024L * 1024L
        const val MAX_SINGLE_ENTRY_BYTES = 12L * 1024L * 1024L
        const val MAX_ARCHIVE_ENTRIES = 256
        const val MAX_ENTRY_PATH_LENGTH = 180
        const val MAX_EXPANSION_RATIO = 100L
        const val MIN_RATIO_BASE_BYTES = 64L * 1024L
        private const val COPY_BUFFER_BYTES = 16 * 1024
    }
}

class PetPackInstallException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
