package com.asianmobile.emojibattery.shimeji.pet.pack

import android.graphics.BitmapFactory
import java.io.File
import javax.inject.Inject

class PetPackDiskLoader @Inject constructor(
    private val parser: PetPackManifestParser,
    private val validator: PetPackValidator
) {
    fun load(directory: File): PetPack {
        val manifestFile = File(directory, PET_PACK_MANIFEST_FILE)
        if (!manifestFile.isFile || manifestFile.length() > MAX_MANIFEST_BYTES) {
            throw PetPackInstallException("Pack manifest is missing or too large")
        }
        val manifest = parser.parse(manifestFile.readText(Charsets.UTF_8))
        val imagePaths = manifest.clips.values
            .flatMap(PetPackClip::frames)
            .map(PetPackFrame::file)
            .toSet()
        val images = imagePaths.associateWith { path -> readImageInfo(directory, path) }
        val validation = validator.validate(manifest, images)
        if (!validation.isValid) {
            throw PetPackInstallException(validation.errors.joinToString(separator = "; "))
        }
        val totalPixels = images.values.sumOf { it.width.toLong() * it.height.toLong() }
        if (totalPixels > MAX_TOTAL_IMAGE_PIXELS) {
            throw PetPackInstallException("Pack images exceed the pixel budget")
        }
        return PetPack(
            manifest = manifest,
            source = PetPackSource.Installed(directory.canonicalPath)
        )
    }

    private fun readImageInfo(directory: File, path: String): PetImageInfo {
        if (!validator.isSafeAssetPath(path)) {
            throw PetPackInstallException("Pack references an unsafe image path")
        }
        val file = File(directory, path)
        val directoryPrefix = directory.canonicalPath + File.separator
        if (!file.canonicalPath.startsWith(directoryPrefix) || !file.isFile) {
            throw PetPackInstallException("Pack image is missing")
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth !in 1..MAX_IMAGE_SIDE || options.outHeight !in 1..MAX_IMAGE_SIDE) {
            throw PetPackInstallException("Pack image dimensions are invalid")
        }
        val extension = file.extension.lowercase()
        val expectedMimeType = when (extension) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> null
        }
        if (expectedMimeType == null || options.outMimeType != expectedMimeType) {
            throw PetPackInstallException("Pack image type does not match its extension")
        }
        return PetImageInfo(
            width = options.outWidth,
            height = options.outHeight,
            byteCount = options.outWidth.toLong() * options.outHeight.toLong() * BYTES_PER_PIXEL
        )
    }

    companion object {
        const val MAX_MANIFEST_BYTES = 256L * 1024L
        const val MAX_IMAGE_SIDE = 4_096
        const val MAX_TOTAL_IMAGE_PIXELS = 16L * 1024L * 1024L
        private const val BYTES_PER_PIXEL = 4L
    }
}
