package com.asianmobile.emojibattery.shimeji.pet.pack

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector

data class PetPackManifest(
    val schemaVersion: Int,
    val id: String,
    val version: Int,
    val name: String,
    val author: String?,
    val canvas: PetPackCanvas,
    val anchor: PetPackAnchor,
    val interaction: PetPackInteraction,
    val clips: Map<PetAction, PetPackClip>,
    val speechAnchor: PetPackAnchor? = null
)

data class PetPackCanvas(
    val width: Int,
    val height: Int,
    val defaultScale: Float
)

data class PetPackAnchor(
    val x: Float,
    val y: Float
)

data class PetPackInteraction(
    val tapAction: PetAction
)

data class PetPackClip(
    val action: PetAction,
    val loops: Boolean,
    val nextAction: PetAction?,
    val frames: List<PetPackFrame>
)

data class PetPackFrame(
    val file: String,
    val rect: PetPackFrameRect,
    val durationMillis: Long,
    val velocity: PetVector
)

data class PetPackFrameRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class PetImageInfo(
    val width: Int,
    val height: Int,
    val byteCount: Long
)

sealed interface PetPackSource {
    data object BuiltIn : PetPackSource
    data class Installed(val directoryPath: String) : PetPackSource
}

data class PetPack(
    val manifest: PetPackManifest,
    val source: PetPackSource
) {
    val key: String
        get() = "${manifest.id}@${manifest.version}"
}

data class PetPackValidationResult(
    val errors: List<String>
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

class PetPackFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

const val PET_PACK_SCHEMA_VERSION = 1
const val PET_PACK_MANIFEST_FILE = "manifest.json"
