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
import org.json.JSONObject

@Singleton
class GithubPetCatalogClient @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val catalogCacheDirectory = File(context.filesDir, "pet_catalog").also {
        it.mkdirs()
    }
    private val catalogCacheFile = File(catalogCacheDirectory, "pets.json")
    private val catalogMetadataFile = File(catalogCacheDirectory, "metadata.json")
    private val archiveCacheDirectory = File(context.cacheDir, "pet_catalog_archives").also {
        it.mkdirs()
    }

    fun readCachedCatalog(): CachedPetCatalog? = runCatching {
        val json = catalogCacheFile.takeIf(File::isFile)?.readText(Charsets.UTF_8)
            ?: return@runCatching null
        CachedPetCatalog(json = json, metadata = readCatalogMetadata())
    }.getOrNull()

    fun readCatalogCacheMetadata(): PetCatalogCacheMetadata = readCatalogMetadata()

    fun shouldRefreshCatalog(metadata: PetCatalogCacheMetadata): Boolean =
        PetCatalogRefreshPolicy.shouldRefresh(
            nowEpochMillis = System.currentTimeMillis(),
            lastValidatedAtEpochMillis = metadata.lastValidatedAtEpochMillis,
            retryAfterEpochMillis = metadata.retryAfterEpochMillis
        )

    fun canForceRefreshCatalog(metadata: PetCatalogCacheMetadata): Boolean =
        PetCatalogRefreshPolicy.canForceRefresh(
            nowEpochMillis = System.currentTimeMillis(),
            retryAfterEpochMillis = metadata.retryAfterEpochMillis
        )

    fun fetchCatalog(etag: String?): PetCatalogFetchResult = runCatching {
        val connection = openConnection(PetServerConfig.CATALOG_URL)
        try {
            etag?.takeIf(String::isNotBlank)?.let { value ->
                connection.setRequestProperty(IF_NONE_MATCH_HEADER, value)
            }
            connection.connect()
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> PetCatalogFetchResult.Updated(
                    json = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    },
                    etag = connection.getHeaderField(ETAG_HEADER)
                )

                HttpURLConnection.HTTP_NOT_MODIFIED -> PetCatalogFetchResult.NotModified(
                    etag = connection.getHeaderField(ETAG_HEADER) ?: etag
                )

                HTTP_TOO_MANY_REQUESTS, HttpURLConnection.HTTP_FORBIDDEN ->
                    PetCatalogFetchResult.RateLimited(
                        retryAtEpochMillis = PetCatalogRefreshPolicy.rateLimitRetryAt(
                            nowEpochMillis = System.currentTimeMillis(),
                            retryAfterSeconds = connection.getHeaderField(RETRY_AFTER_HEADER),
                            rateLimitResetEpochSeconds = connection.getHeaderField(
                                RATE_LIMIT_RESET_HEADER
                            )
                        )
                    )

                else -> PetCatalogFetchResult.Failed
            }
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(PetCatalogFetchResult.Failed)

    fun cacheCatalog(json: String, etag: String?): Boolean = runCatching {
        val temporary = File(catalogCacheDirectory, "pets.json.tmp")
        temporary.writeText(json, Charsets.UTF_8)
        if (catalogCacheFile.exists() && !catalogCacheFile.delete()) {
            error("Unable to replace cached catalog")
        }
        if (!temporary.renameTo(catalogCacheFile)) {
            error("Unable to promote cached catalog")
        }
        if (
            !writeCatalogMetadata(
                PetCatalogCacheMetadata(
                    etag = etag,
                    lastValidatedAtEpochMillis = System.currentTimeMillis(),
                    retryAfterEpochMillis = 0L
                )
            )
        ) {
            error("Unable to cache catalog metadata")
        }
        true
    }.getOrDefault(false)

    fun markCatalogNotModified(etag: String?): Boolean = writeCatalogMetadata(
        readCatalogMetadata().copy(
            etag = etag,
            lastValidatedAtEpochMillis = System.currentTimeMillis(),
            retryAfterEpochMillis = 0L
        )
    )

    fun deferCatalogRefresh(retryAtEpochMillis: Long): Boolean = writeCatalogMetadata(
        readCatalogMetadata().copy(retryAfterEpochMillis = retryAtEpochMillis)
    )

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

    private fun readCatalogMetadata(): PetCatalogCacheMetadata = runCatching {
        val document = catalogMetadataFile.takeIf(File::isFile)
            ?.readText(Charsets.UTF_8)
            ?.let(::JSONObject)
            ?: return@runCatching PetCatalogCacheMetadata()
        PetCatalogCacheMetadata(
            etag = document.opt(ETAG_JSON_KEY) as? String,
            lastValidatedAtEpochMillis = document.optLong(LAST_VALIDATED_AT_JSON_KEY),
            retryAfterEpochMillis = document.optLong(RETRY_AFTER_JSON_KEY)
        )
    }.getOrDefault(PetCatalogCacheMetadata())

    private fun writeCatalogMetadata(metadata: PetCatalogCacheMetadata): Boolean = runCatching {
        val document = JSONObject()
            .put(ETAG_JSON_KEY, metadata.etag ?: JSONObject.NULL)
            .put(LAST_VALIDATED_AT_JSON_KEY, metadata.lastValidatedAtEpochMillis)
            .put(RETRY_AFTER_JSON_KEY, metadata.retryAfterEpochMillis)
        val temporary = File(catalogCacheDirectory, "metadata.json.tmp")
        temporary.writeText(document.toString(), Charsets.UTF_8)
        if (catalogMetadataFile.exists() && !catalogMetadataFile.delete()) {
            error("Unable to replace catalog metadata")
        }
        if (!temporary.renameTo(catalogMetadataFile)) {
            error("Unable to promote catalog metadata")
        }
        true
    }.getOrDefault(false)

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json, application/octet-stream")
            setRequestProperty("User-Agent", USER_AGENT)
            token().takeIf(String::isNotBlank)?.let { token ->
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

    private fun <T> request(url: String, block: (HttpURLConnection) -> T): T? = runCatching {
        val connection = openConnection(url)
        try {
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
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val IF_NONE_MATCH_HEADER = "If-None-Match"
        const val ETAG_HEADER = "ETag"
        const val RETRY_AFTER_HEADER = "Retry-After"
        const val RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset"
        const val ETAG_JSON_KEY = "etag"
        const val LAST_VALIDATED_AT_JSON_KEY = "lastValidatedAtEpochMillis"
        const val RETRY_AFTER_JSON_KEY = "retryAfterEpochMillis"
    }
}

data class CachedPetCatalog(
    val json: String,
    val metadata: PetCatalogCacheMetadata
)

data class PetCatalogCacheMetadata(
    val etag: String? = null,
    val lastValidatedAtEpochMillis: Long = 0L,
    val retryAfterEpochMillis: Long = 0L
)

sealed interface PetCatalogFetchResult {
    data class Updated(
        val json: String,
        val etag: String?
    ) : PetCatalogFetchResult

    data class NotModified(val etag: String?) : PetCatalogFetchResult

    data class RateLimited(val retryAtEpochMillis: Long) : PetCatalogFetchResult

    data object Failed : PetCatalogFetchResult
}
