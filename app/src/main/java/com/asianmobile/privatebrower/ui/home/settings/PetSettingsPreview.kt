package com.asianmobile.privatebrower.ui.home.settings

import com.asianmobile.privatebrower.pet.pack.PetPack
import com.asianmobile.privatebrower.pet.pack.PetPackSource
import java.io.File

internal fun PetPack.previewImagePath(): String? {
    val installed = source as? PetPackSource.Installed ?: return null
    val firstFrame = manifest.clips.values.firstOrNull()?.frames?.firstOrNull() ?: return null
    return File(installed.directoryPath, firstFrame.file).absolutePath
}
