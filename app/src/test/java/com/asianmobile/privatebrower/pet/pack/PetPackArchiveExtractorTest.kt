package com.asianmobile.privatebrower.pet.pack

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PetPackArchiveExtractorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val extractor = PetPackArchiveExtractor()

    @Test
    fun `extract writes supported entries inside destination`() {
        val archive = zipOf(
            PET_PACK_MANIFEST_FILE to "{}".toByteArray(),
            "sprites/cat.png" to byteArrayOf(1, 2, 3)
        )
        val destination = temporaryFolder.newFolder("output")

        extractor.extract(archive, destination)

        assertArrayEquals(byteArrayOf(1, 2, 3), File(destination, "sprites/cat.png").readBytes())
    }

    @Test
    fun `extract rejects path traversal before writing outside destination`() {
        val archive = zipOf("../outside.png" to byteArrayOf(1))
        val destination = temporaryFolder.newFolder("safe")

        assertThrows(PetPackInstallException::class.java) {
            extractor.extract(archive, destination)
        }
    }

    @Test
    fun `extract rejects unsupported payload`() {
        val archive = zipOf("payload.dex" to byteArrayOf(1))

        assertThrows(PetPackInstallException::class.java) {
            extractor.extract(archive, temporaryFolder.newFolder("unsupported"))
        }
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): File {
        val archive = temporaryFolder.newFile("pack-${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(archive)).use { output ->
            entries.forEach { (name, bytes) ->
                output.putNextEntry(ZipEntry(name))
                output.write(bytes)
                output.closeEntry()
            }
        }
        return archive
    }
}
