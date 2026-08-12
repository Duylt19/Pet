package com.asianmobile.emojibattery.shimeji.data.remote

import android.content.Context
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
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
class GithubBatteryCatalogClient @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val catalogDirectory = File(context.filesDir, "battery_catalog_remote").also {
        it.mkdirs()
    }
    private val catalogFile = File(catalogDirectory, "batteries.json")
    private val metadataFile = File(catalogDirectory, "metadata.json")
    private val assetDirectory = File(context.filesDir, "battery_catalog_assets").also {
        it.mkdirs()
    }

    fun readCachedCatalog(): CachedBatteryCatalog? = runCatching {
        val json = catalogFile.takeIf(File::isFile)?.readText(Charsets.UTF_8)
            ?: return@runCatching null
        CachedBatteryCatalog(json, readMetadata())
    }.getOrNull()

    fun readCatalogCacheMetadata(): BatteryCatalogCacheMetadata = readMetadata()

    fun shouldRefreshCatalog(metadata: BatteryCatalogCacheMetadata): Boolean =
        PetCatalogRefreshPolicy.shouldRefresh(
            nowEpochMillis = System.currentTimeMillis(),
            lastValidatedAtEpochMillis = metadata.lastValidatedAtEpochMillis,
            retryAfterEpochMillis = metadata.retryAfterEpochMillis
        )

    fun fetchCatalog(etag: String?): BatteryCatalogFetchResult = runCatching {
        val connection = openConnection(BatteryServerConfig.CATALOG_URL)
        try {
            etag?.takeIf(String::isNotBlank)?.let {
                connection.setRequestProperty(IF_NONE_MATCH_HEADER, it)
            }
            connection.connect()
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> BatteryCatalogFetchResult.Updated(
                    json = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    },
                    etag = connection.getHeaderField(ETAG_HEADER)
                )

                HttpURLConnection.HTTP_NOT_MODIFIED -> BatteryCatalogFetchResult.NotModified(
                    connection.getHeaderField(ETAG_HEADER) ?: etag
                )

                HTTP_TOO_MANY_REQUESTS, HttpURLConnection.HTTP_FORBIDDEN ->
                    BatteryCatalogFetchResult.RateLimited(
                        PetCatalogRefreshPolicy.rateLimitRetryAt(
                            nowEpochMillis = System.currentTimeMillis(),
                            retryAfterSeconds = connection.getHeaderField(RETRY_AFTER_HEADER),
                            rateLimitResetEpochSeconds = connection.getHeaderField(
                                RATE_LIMIT_RESET_HEADER
                            )
                        )
                    )

                else -> BatteryCatalogFetchResult.Failed
            }
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(BatteryCatalogFetchResult.Failed)

    fun cacheCatalog(json: String, etag: String?): Boolean = runCatching {
        val temporary = File(catalogDirectory, "batteries.json.tmp")
        temporary.writeText(json, Charsets.UTF_8)
        replace(temporary, catalogFile)
        check(
            writeMetadata(
                BatteryCatalogCacheMetadata(
                    etag = etag,
                    lastValidatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        )
        true
    }.getOrDefault(false)

    fun markCatalogNotModified(etag: String?): Boolean = writeMetadata(
        readMetadata().copy(
            etag = etag,
            lastValidatedAtEpochMillis = System.currentTimeMillis(),
            retryAfterEpochMillis = 0L
        )
    )

    fun deferCatalogRefresh(retryAtEpochMillis: Long): Boolean = writeMetadata(
        readMetadata().copy(retryAfterEpochMillis = retryAtEpochMillis)
    )

    @Synchronized
    fun materializeAsset(
        url: String,
        relativePath: String,
        expectedSizeBytes: Long,
        expectedSha256: String
    ): File? {
        if (expectedSizeBytes !in 1..MAX_ASSET_BYTES) return null
        val extension = relativePath.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { SAFE_EXTENSION.matches(it) }
            ?: return null
        val finalFile = File(assetDirectory, "$expectedSha256.$extension")
        if (
            finalFile.isFile &&
            verify(finalFile, expectedSizeBytes, expectedSha256)
        ) {
            return finalFile
        }

        val temporary = File(assetDirectory, "$expectedSha256.tmp")
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
                        if (totalBytes > expectedSizeBytes || totalBytes > MAX_ASSET_BYTES) {
                            error("Battery asset exceeds its declared size")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            totalBytes == expectedSizeBytes && digest.digest().toHex() == expectedSha256
        } == true
        if (!downloaded) {
            temporary.delete()
            return null
        }
        return runCatching {
            replace(temporary, finalFile)
            finalFile
        }.getOrElse {
            temporary.delete()
            null
        }
    }

    private fun verify(
        file: File,
        expectedSizeBytes: Long,
        expectedSha256: String
    ): Boolean {
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

    private fun readMetadata(): BatteryCatalogCacheMetadata = runCatching {
        val root = metadataFile.takeIf(File::isFile)
            ?.readText(Charsets.UTF_8)
            ?.let(::JSONObject)
            ?: return@runCatching BatteryCatalogCacheMetadata()
        BatteryCatalogCacheMetadata(
            etag = root.opt(ETAG_JSON_KEY) as? String,
            lastValidatedAtEpochMillis = root.optLong(LAST_VALIDATED_AT_JSON_KEY),
            retryAfterEpochMillis = root.optLong(RETRY_AFTER_JSON_KEY)
        )
    }.getOrDefault(BatteryCatalogCacheMetadata())

    private fun writeMetadata(metadata: BatteryCatalogCacheMetadata): Boolean = runCatching {
        val root = JSONObject()
            .put(ETAG_JSON_KEY, metadata.etag ?: JSONObject.NULL)
            .put(LAST_VALIDATED_AT_JSON_KEY, metadata.lastValidatedAtEpochMillis)
            .put(RETRY_AFTER_JSON_KEY, metadata.retryAfterEpochMillis)
        val temporary = File(catalogDirectory, "metadata.json.tmp")
        temporary.writeText(root.toString(), Charsets.UTF_8)
        replace(temporary, metadataFile)
        true
    }.getOrDefault(false)

    private fun replace(source: File, target: File) {
        if (target.exists() && !target.delete()) error("Unable to replace ${target.name}")
        if (!source.renameTo(target)) error("Unable to promote ${source.name}")
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json, application/octet-stream")
            setRequestProperty("User-Agent", USER_AGENT)
            token().takeIf(String::isNotBlank)?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }

    private fun <T> request(url: String, block: (HttpURLConnection) -> T): T? = runCatching {
        val connection = openConnection(url)
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Battery server returned HTTP ${connection.responseCode}")
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
        const val MAX_ASSET_BYTES = 5L * 1024L * 1024L
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
        val SAFE_EXTENSION = Regex("(?:png|webp|gif|json)")
    }
}

data class CachedBatteryCatalog(
    val json: String,
    val metadata: BatteryCatalogCacheMetadata
)

data class BatteryCatalogCacheMetadata(
    val etag: String? = null,
    val lastValidatedAtEpochMillis: Long = 0L,
    val retryAfterEpochMillis: Long = 0L
)

sealed interface BatteryCatalogFetchResult {
    data class Updated(val json: String, val etag: String?) : BatteryCatalogFetchResult
    data class NotModified(val etag: String?) : BatteryCatalogFetchResult
    data class RateLimited(val retryAtEpochMillis: Long) : BatteryCatalogFetchResult
    data object Failed : BatteryCatalogFetchResult
}
