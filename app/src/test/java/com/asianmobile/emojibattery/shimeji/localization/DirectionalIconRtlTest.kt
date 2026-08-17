package com.asianmobile.emojibattery.shimeji.localization

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionalIconRtlTest {
    @Test
    fun `navigation vectors mirror in rtl locales`() {
        directionalIcons.forEach { fileName ->
            val vector = resourceFile("drawable/$fileName").readText()
            assertTrue(
                "$fileName must mirror when layout direction is RTL",
                vector.contains("android:autoMirrored=\"true\"")
            )
        }
    }

    private fun resourceFile(relativePath: String): File {
        val resourceRoot = sequenceOf(File("src/main/res"), File("app/src/main/res"))
            .firstOrNull(File::isDirectory)
            ?: error("Cannot find app resources from ${File(".").absolutePath}")
        return resourceRoot.resolve(relativePath).also { file ->
            check(file.isFile) { "Cannot find $relativePath in ${resourceRoot.absolutePath}" }
        }
    }

    private companion object {
        val directionalIcons = listOf(
            "ic_arrow_back.xml",
            "ic_arrow_right.xml",
            "ic_chevron_right.xml",
            "ic_favorite_recent_back.xml",
            "ic_home_chevron.xml",
            "ic_pet_room_back.xml",
            "ic_setting_chevron_right_v2.xml",
            "ic_statusbar_more.xml",
        )
    }
}
