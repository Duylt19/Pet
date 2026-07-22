package com.asianmobile.privatebrower.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File

import android.os.Environment
import android.provider.DocumentsContract

object VaultUtils {

    fun deleteOriginalFile(context: Context, uri: Uri) {
        runCatching {
            val filePath = getFilePathFromUri(context, uri)
            var deletedByFile = false
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    deletedByFile = file.delete()
                    Log.d("TAG", "deleteOriginalFile by absolute path: $deletedByFile")
                }
            }

            // Nếu File API xóa không thành công (VD Android ver thấp bị khóa SAF), fallback dùng ContentResolver.
            if (!deletedByFile) {
                context.contentResolver.delete(uri, null, null)
                Log.d("TAG", "deleteOriginalFile by ContentResolver: DELETED")
            }
        }.onFailure { e ->
            Log.e("TAG", "FAIL TO DELETE: ${e.message}")
        }
    }

    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path

        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split.firstOrNull() ?: ""

            when {
                "image" == type || "video" == type || "audio" == type || "document" == type -> {
                    val contentUri = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Files.getContentUri("external")
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split.getOrNull(1) ?: "")
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
                "primary".equals(type, ignoreCase = true) -> {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split.getOrNull(1)
                }
            }
        }

        if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        }
        return null
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        if (uri == null) return null
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                } else null
            }
        }.getOrNull()
    }

    fun ensureNomediaFile(vaultRootDir: File) {
        if (!vaultRootDir.exists()) return
        val nomediaFile = File(vaultRootDir, ".nomedia")
        if (!nomediaFile.exists()) {
            runCatching { nomediaFile.createNewFile() }
        }
    }
}


