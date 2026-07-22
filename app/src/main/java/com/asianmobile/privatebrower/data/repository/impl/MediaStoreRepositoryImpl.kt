package com.asianmobile.privatebrower.data.repository.impl

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.app.RecoverableSecurityException
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.asianmobile.privatebrower.data.repository.MediaItem
import com.asianmobile.privatebrower.data.repository.MediaDeleteResult
import com.asianmobile.privatebrower.data.repository.MediaSizes
import com.asianmobile.privatebrower.data.repository.MediaSource
import com.asianmobile.privatebrower.data.repository.MediaStoreRepository
import com.asianmobile.privatebrower.utils.permission.BroadStorageAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.absoluteValue

class MediaStoreRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MediaStoreRepository {

    override suspend fun queryMediaSizes(): MediaSizes = withContext(Dispatchers.IO) {
        MediaSizes(
            imageBytes = queryTotalSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            videoBytes = queryTotalSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audioBytes = queryTotalSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            fileBytes = runCatching { queryFiles().sumOf(MediaItem::sizeBytes) }.getOrDefault(0L)
        )
    }

    override suspend fun queryImages(): List<MediaItem> = queryCollection(
        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        fallbackMimeType = "image/*",
        widthColumn = MediaStore.Images.Media.WIDTH,
        heightColumn = MediaStore.Images.Media.HEIGHT
    )

    override suspend fun queryVideos(): List<MediaItem> = queryCollection(
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        fallbackMimeType = "video/*",
        durationColumn = MediaStore.Video.Media.DURATION,
        widthColumn = MediaStore.Video.Media.WIDTH,
        heightColumn = MediaStore.Video.Media.HEIGHT
    )

    override suspend fun queryAudio(): List<MediaItem> = queryCollection(
        collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        fallbackMimeType = "audio/*",
        durationColumn = MediaStore.Audio.Media.DURATION,
        artistColumn = MediaStore.Audio.Media.ARTIST
    )

    override suspend fun queryFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val hasBroadStorageAccess = BroadStorageAccess.isGranted(context)
        val excludedMediaTypes = fileCategoryExcludedMediaTypes()
        val nonMediaSelection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} NOT IN (" +
            excludedMediaTypes.joinToString(separator = ",") { "?" } +
            ")"
        val nonMediaSelectionArgs = excludedMediaTypes
            .map(Int::toString)
            .toTypedArray()
        val mediaStoreItems = when {
            hasBroadStorageAccess -> queryCollection(
                collection = MediaStore.Files.getContentUri(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.VOLUME_EXTERNAL
                    } else {
                        "external"
                    }
                ),
                fallbackMimeType = "application/octet-stream",
                selection = nonMediaSelection,
                selectionArgs = nonMediaSelectionArgs
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> queryCollection(
                collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                fallbackMimeType = "application/octet-stream"
            )
            else -> queryCollection(
                collection = MediaStore.Files.getContentUri("external"),
                fallbackMimeType = "application/octet-stream",
                selection = nonMediaSelection,
                selectionArgs = nonMediaSelectionArgs
            )
        }.filterNot { item -> isMediaMimeType(item.mimeType) }

        val indexedAppDownloads = mediaStoreItems
            .asSequence()
            .filter { item ->
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    item.relativePath.isAppDownloadRelativePath()
            }
            .mapTo(hashSetOf()) { it.name to it.sizeBytes }
        val appDownloads = appDownloadDirectory()
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter(File::isFile)
            .filter { file -> !isMediaMimeType(file.resolvedMimeType()) }
            .filterNot { it.name to it.length() in indexedAppDownloads }
            .map(::fileToMediaItem)
            .toList()

        (mediaStoreItems + appDownloads).sortedWith(
            compareByDescending<MediaItem>(MediaItem::newestTimestamp)
                .thenByDescending(MediaItem::dateModified)
                .thenByDescending(MediaItem::id)
        )
    }

    override suspend fun delete(item: MediaItem): MediaDeleteResult = withContext(Dispatchers.IO) {
        if (item.source == MediaSource.APP_DOWNLOAD && item.path.isNotBlank()) {
            return@withContext if (File(item.path).delete()) {
                MediaDeleteResult.Success
            } else {
                MediaDeleteResult.Failed
            }
        }

        val uri = item.uri ?: return@withContext MediaDeleteResult.Failed
        try {
            if (context.contentResolver.delete(uri, null, null) > 0) {
                MediaDeleteResult.Success
            } else {
                MediaDeleteResult.Failed
            }
        } catch (error: RecoverableSecurityException) {
            MediaDeleteResult.RequiresPermission(error.userAction.actionIntent.intentSender)
        } catch (_: SecurityException) {
            // Android 11+ uses a MediaStore delete request when direct write access is denied.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                shouldUseMediaStoreDeleteRequest(
                    sdkInt = Build.VERSION.SDK_INT,
                    source = item.source,
                    uriAuthority = uri.authority
                )
            ) {
                runCatching {
                    MediaStore.createDeleteRequest(
                        context.contentResolver,
                        listOf(uri)
                    ).intentSender
                }.fold(
                    onSuccess = { MediaDeleteResult.RequiresPermission(it) },
                    onFailure = { MediaDeleteResult.Failed }
                )
            } else {
                MediaDeleteResult.Failed
            }
        }
    }

    private suspend fun queryCollection(
        collection: Uri,
        fallbackMimeType: String,
        durationColumn: String? = null,
        artistColumn: String? = null,
        widthColumn: String? = null,
        heightColumn: String? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
            durationColumn?.let(::add)
            artistColumn?.let(::add)
            widthColumn?.let(::add)
            heightColumn?.let(::add)
        }.distinct().toTypedArray()

        buildList {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC, " +
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC, " +
                    "${MediaStore.MediaColumns._ID} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateAddedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val relativePathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val durationIndex = durationColumn?.let(cursor::getColumnIndex) ?: -1
                val artistIndex = artistColumn?.let(cursor::getColumnIndex) ?: -1
                val widthIndex = widthColumn?.let(cursor::getColumnIndex) ?: -1
                val heightIndex = heightColumn?.let(cursor::getColumnIndex) ?: -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    add(
                        MediaItem(
                            id = id,
                            name = cursor.stringOrEmpty(nameIndex),
                            path = "",
                            uri = ContentUris.withAppendedId(collection, id),
                            mimeType = cursor.stringOrEmpty(mimeIndex).ifBlank { fallbackMimeType },
                            sizeBytes = cursor.longOrZero(sizeIndex),
                            dateModified = cursor.longOrZero(dateIndex),
                            dateAdded = cursor.longOrZero(dateAddedIndex),
                            duration = cursor.longOrZero(durationIndex),
                            artist = cursor.stringOrEmpty(artistIndex),
                            width = cursor.intOrZero(widthIndex),
                            height = cursor.intOrZero(heightIndex),
                            relativePath = cursor.stringOrEmpty(relativePathIndex)
                        )
                    )
                }
            }
        }
    }

    private fun queryTotalSize(collection: Uri): Long = runCatching {
        var total = 0L
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) total += cursor.longOrZero(sizeIndex)
        }
        total
    }.getOrDefault(0L)

    private fun appDownloadDirectory(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        APP_DOWNLOAD_FOLDER
    )

    private fun fileToMediaItem(file: File): MediaItem {
        return MediaItem(
            id = -file.absolutePath.hashCode().toLong().absoluteValue.coerceAtLeast(1L),
            name = file.name,
            path = file.absolutePath,
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            ),
            mimeType = file.resolvedMimeType(),
            sizeBytes = file.length(),
            dateModified = file.lastModified() / 1000L,
            source = MediaSource.APP_DOWNLOAD,
            dateAdded = file.lastModified() / 1000L
        )
    }

    private fun android.database.Cursor.stringOrEmpty(index: Int): String =
        if (index >= 0 && !isNull(index)) getString(index).orEmpty() else ""

    private fun android.database.Cursor.longOrZero(index: Int): Long =
        if (index >= 0 && !isNull(index)) getLong(index) else 0L

    private fun android.database.Cursor.intOrZero(index: Int): Int =
        if (index >= 0 && !isNull(index)) getInt(index) else 0

    private fun File.resolvedMimeType(): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: "application/octet-stream"

    private fun String.isAppDownloadRelativePath(): Boolean =
        replace('\\', '/')
            .trim('/')
            .equals(APP_DOWNLOAD_RELATIVE_PATH, ignoreCase = true)

    private companion object {
        const val APP_DOWNLOAD_FOLDER = "PrivateBrowser"
        val APP_DOWNLOAD_RELATIVE_PATH =
            "${Environment.DIRECTORY_DOWNLOADS}/$APP_DOWNLOAD_FOLDER"
    }
}

internal fun fileCategoryExcludedMediaTypes(): IntArray = intArrayOf(
    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
    MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
)

internal fun isMediaMimeType(mimeType: String): Boolean =
    mimeType.startsWith("image/") ||
        mimeType.startsWith("video/") ||
        mimeType.startsWith("audio/")

internal fun shouldUseMediaStoreDeleteRequest(
    sdkInt: Int,
    source: MediaSource,
    uriAuthority: String?
): Boolean =
    sdkInt >= Build.VERSION_CODES.R &&
        source == MediaSource.MEDIA_STORE &&
        uriAuthority == MediaStore.AUTHORITY
