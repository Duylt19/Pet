package com.asianmobile.emojibattery.shimeji.pet.pack

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPackValidatorTest {
    private val parser = PetPackManifestParser()
    private val validator = PetPackValidator()
    private val imageInfo = PetImageInfo(width = 128, height = 128, byteCount = 65_536)

    @Test
    fun `valid pack passes validation`() {
        val result = validator.validate(
            parser.parse(validManifestJson()),
            mapOf("sprites/cat.png" to imageInfo)
        )

        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun `asset traversal path is rejected`() {
        val manifest = parser.parse(validManifestJson("../cat.png"))
        val result = validator.validate(manifest, mapOf("../cat.png" to imageInfo))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "unsafe file path" in it })
    }

    @Test
    fun `frame outside bitmap is rejected`() {
        val manifest = parser.parse(validManifestJson())
        val result = validator.validate(
            manifest,
            mapOf("sprites/cat.png" to imageInfo.copy(width = 64, height = 64))
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "outside its image" in it })
    }

    @Test
    fun `unknown schema and missing image are rejected together`() {
        val manifest = parser.parse(validManifestJson()).copy(schemaVersion = 99)
        val result = validator.validate(manifest, emptyMap())

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "Unsupported schemaVersion" in it })
        assertTrue(result.errors.any { "image is missing" in it })
    }

    @Test
    fun `speech anchor outside normalized range is rejected`() {
        val manifest = parser.parse(validManifestJson()).copy(
            speechAnchor = PetPackAnchor(x = -0.1f, y = 1.1f)
        )
        val result = validator.validate(manifest, mapOf("sprites/cat.png" to imageInfo))

        assertFalse(result.isValid)
        assertTrue(result.errors.any { "Speech anchor must be normalized" in it })
    }
}
