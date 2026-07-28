package com.asianmobile.emojibattery.shimeji.data.remote

import android.content.Context
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackArchiveExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubPetCatalogClient @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val catalogCacheDirectory = File(context.filesDir, "pet_catalog").also {
        it.mkdirs()
    }
    private val catalogCacheFile = File(catalogCacheDirectory, "pets.json")
    private val archiveCacheDirectory = File(context.cacheDir, "pet_catalog_archives").also {
        it.mkdirs()
    }

    fun fetchCatalogJson(): String? = request(PetServerConfig.CATALOG_URL) { connection ->
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun readCachedCatalogJson(): String? =
        runCatching { catalogCacheFile.takeIf(File::isFile)?.readText(Charsets.UTF_8) }.getOrNull()

    fun cacheCatalogJson(json: String): Boolean = runCatching {
        val temporary = File(catalogCacheDirectory, "pets.json.tmp")
        temporary.writeText(json, Charsets.UTF_8)
        if (catalogCacheFile.exists() && !catalogCacheFile.delete()) {
            error("Unable to replace cached catalog")
        }
        if (!temporary.renameTo(catalogCacheFile)) {
            error("Unable to promote cached catalog")
        }
        true
    }.getOrDefault(false)

    fun downloadArchive(
        petId: Int,
        url: String,
        expectedSizeBytes: Long,
        expectedSha256: String
    ): File? {
        if (expectedSizeBytes !in 1..PetPackArchiveExtractor.MAX_ARCHIVE_BYTES) return null
        val finalFile = File(archiveCacheDirectory, "$petId-$expectedSha256.zip")
        if (finalFile.isFile && verify(finalFile, expectedSizeBytes, expectedSha256)) {
            return finalFile
        }
        archiveCacheDirectory.listFiles { file ->
            file.name.startsWith("$petId-") && file != finalFile
        }?.forEach(File::delete)

        val temporary = File(archiveCacheDirectory, "$petId.tmp")
        temporary.delete()
        val downloaded = request(url) { connection ->
            val digest = MessageDigest.getInstance(SHA_256)
            var totalBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > expectedSizeBytes ||
                            totalBytes > PetPackArchiveExtractor.MAX_ARCHIVE_BYTES
                        ) {
                            error("Pet archive exceeds its declared size")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            totalBytes == expectedSizeBytes &&
                digest.digest().toHex() == expectedSha256
        } == true
        if (!downloaded) {
            temporary.delete()
            return null
        }
        finalFile.delete()
        if (!temporary.renameTo(finalFile)) {
            temporary.delete()
            return null
        }
        return finalFile
    }

    private fun verify(file: File, expectedSizeBytes: Long, expectedSha256: String): Boolean {
        if (file.length() != expectedSizeBytes) return false
        val digest = MessageDigest.getInstance(SHA_256)
        file.inputStream().use { input ->
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex() == expectedSha256
    }

    private fun <T> request(url: String, block: (HttpURLConnection) -> T): T? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/json, application/octet-stream")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            token().takeIf(String::isNotBlank)?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Pet server returned HTTP ${connection.responseCode}")
            }
            block(connection)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun token(): String =
        SafeRemoteConfig.getSensitiveString(PetServerConfig.REMOTE_CONFIG_TOKEN_KEY).trim()

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 45_000
        const val COPY_BUFFER_BYTES = 16 * 1024
        const val SHA_256 = "SHA-256"
        const val USER_AGENT = "CutePet-Android"
    }
}
