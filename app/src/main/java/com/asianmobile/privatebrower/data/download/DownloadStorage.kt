package com.asianmobile.privatebrower.data.download

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class PublishedDownload(
    val locator: String,
    val displayName: String,
    val sizeBytes: Long
)

object DownloadStorage {
    private const val DOWNLOAD_FOLDER = "PrivateBrowser"
    private const val PENDING_FOLDER = ".pending"
    private val mediaStoreRelativePath =
        "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_FOLDER/"

    fun pendingPath(context: Context, fileName: String): String =
        File(pendingDirectory(context), fileName).absolutePath

    fun pendingFile(context: Context, currentLocator: String, fileName: String): File {
        if (currentLocator.isNotBlank() && !isContentLocator(currentLocator)) {
            val current = File(currentLocator)
            val publicDownloads = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .absolutePath
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                !current.absolutePath.startsWith(publicDownloads)
            ) {
                current.parentFile?.mkdirs()
                return current
            }
        }
        return File(pendingDirectory(context), fileName).apply {
            parentFile?.mkdirs()
        }
    }

    suspend fun publish(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String
    ): PublishedDownload {
        require(source.exists()) { "Downloaded file is missing" }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(context, source, displayName, mimeType)
        } else {
            publishLegacy(context, source, displayName, mimeType)
        }
    }

    fun exists(context: Context, locator: String): Boolean {
        if (locator.isBlank()) return false
        if (!isContentLocator(locator)) return File(locator).exists()
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(locator), "r")?.use { true }
                ?: false
        }.getOrDefault(false)
    }

    fun length(context: Context, locator: String): Long {
        if (locator.isBlank()) return 0L
        if (!isContentLocator(locator)) return File(locator).takeIf(File::exists)?.length() ?: 0L
        val uri = Uri.parse(locator)
        val queriedSize = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) 0L else {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else 0L
                }
            } ?: 0L
        }.getOrDefault(0L)
        if (queriedSize > 0L) return queriedSize
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.coerceAtLeast(0L)
            } ?: 0L
        }.getOrDefault(0L)
    }

    fun delete(context: Context, locator: String): Boolean {
        if (locator.isBlank()) return false
        return if (isContentLocator(locator)) {
            runCatching { context.contentResolver.delete(Uri.parse(locator), null, null) > 0 }
                .getOrDefault(false)
        } else {
            runCatching { File(locator).delete() }.getOrDefault(false)
        }
    }

    fun shareUri(context: Context, locator: String): Uri? {
        if (!exists(context, locator)) return null
        return if (isContentLocator(locator)) {
            Uri.parse(locator)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(locator)
            )
        }
    }

    fun coilModel(locator: String?): Any? {
        val value = locator?.takeIf(String::isNotBlank) ?: return null
        return if (isContentLocator(value)) {
            Uri.parse(value)
        } else {
            File(value).takeIf { file -> file.exists() && file.length() > 0L }
        }
    }

    fun openInputStream(context: Context, locator: String): InputStream? =
        runCatching {
            if (isContentLocator(locator)) {
                context.contentResolver.openInputStream(Uri.parse(locator))
            } else {
                File(locator).takeIf(File::exists)?.inputStream()
            }
        }.getOrNull()

    fun setMediaDataSource(
        retriever: MediaMetadataRetriever,
        context: Context,
        locator: String
    ) {
        if (isContentLocator(locator)) {
            retriever.setDataSource(context, Uri.parse(locator))
        } else {
            retriever.setDataSource(locator)
        }
    }

    fun isContentLocator(locator: String): Boolean =
        locator.startsWith("content://", ignoreCase = true)

    private fun pendingDirectory(context: Context): File {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && hasLegacyWritePermission(context)) {
            return publicDownloadDirectory().apply { mkdirs() }
        }
        val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, Environment.DIRECTORY_DOWNLOADS)
        return File(appDownloads, "$DOWNLOAD_FOLDER/$PENDING_FOLDER").apply { mkdirs() }
    }

    private suspend fun publishToMediaStore(
        context: Context,
        source: File,
        requestedName: String,
        mimeType: String
    ): PublishedDownload {
        val resolver = context.contentResolver
        val displayName = uniqueMediaStoreName(context, requestedName)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                mediaStoreRelativePath
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(resolver.insert(collection, values)) {
            "Unable to create Downloads entry"
        }
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                source.inputStream().use { input -> copyWithCancellation(input, output) }
            } ?: error("Unable to open Downloads output")
            check(
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null
                ) > 0
            ) { "Unable to publish Downloads entry" }
            val size = source.length()
            return PublishedDownload(uri.toString(), displayName, size)
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    private suspend fun publishLegacy(
        context: Context,
        source: File,
        requestedName: String,
        mimeType: String
    ): PublishedDownload {
        val publicDirectory = publicDownloadDirectory()
        val canWritePublicDownloads = hasLegacyWritePermission(context)
        if (canWritePublicDownloads) publicDirectory.mkdirs()
        val sourceIsAlreadyPublic = runCatching {
            source.parentFile?.canonicalFile == publicDirectory.canonicalFile
        }.getOrDefault(false)
        val destination = if (canWritePublicDownloads && !sourceIsAlreadyPublic) {
            uniqueFile(publicDirectory, requestedName)
        } else {
            source
        }
        if (destination.absolutePath != source.absolutePath) {
            try {
                destination.outputStream().use { output ->
                    source.inputStream().use { input -> copyWithCancellation(input, output) }
                }
            } catch (error: Throwable) {
                destination.delete()
                throw error
            }
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(destination.absolutePath),
            arrayOf(mimeType),
            null
        )
        return PublishedDownload(destination.absolutePath, destination.name, destination.length())
    }

    private fun uniqueMediaStoreName(context: Context, requestedName: String): String {
        val dotIndex = requestedName.lastIndexOf('.')
        val base = if (dotIndex > 0) requestedName.substring(0, dotIndex) else requestedName
        val extension = if (dotIndex > 0) requestedName.substring(dotIndex) else ""
        var candidate = requestedName
        var index = 1
        while (mediaStoreNameExists(context, candidate)) {
            candidate = "$base ($index)$extension"
            index++
        }
        return candidate
    }

    private fun mediaStoreNameExists(context: Context, displayName: String): Boolean = runCatching {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(displayName, mediaStoreRelativePath),
            null
        )?.use { cursor -> cursor.moveToFirst() } ?: false
    }.getOrDefault(false)

    private fun uniqueFile(directory: File, requestedName: String): File {
        directory.mkdirs()
        val dotIndex = requestedName.lastIndexOf('.')
        val base = if (dotIndex > 0) requestedName.substring(0, dotIndex) else requestedName
        val extension = if (dotIndex > 0) requestedName.substring(dotIndex) else ""
        var candidate = File(directory, requestedName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$base ($index)$extension")
            index++
        }
        return candidate
    }

    private suspend fun copyWithCancellation(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) return
            output.write(buffer, 0, count)
        }
    }

    private fun publicDownloadDirectory(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        DOWNLOAD_FOLDER
    )

    private fun hasLegacyWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
