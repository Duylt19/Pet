package com.asianmobile.emojibattery.shimeji.pet.pack

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction

class PetPackValidator {
    fun validate(
        manifest: PetPackManifest,
        images: Map<String, PetImageInfo>
    ): PetPackValidationResult {
        val errors = mutableListOf<String>()
        if (manifest.schemaVersion != PET_PACK_SCHEMA_VERSION) {
            errors += "Unsupported schemaVersion ${manifest.schemaVersion}"
        }
        if (!PACK_ID.matches(manifest.id)) errors += "Invalid pack id"
        if (manifest.version !in 1..MAX_PACK_VERSION) errors += "Invalid pack version"
        if (manifest.name.isBlank() || manifest.name.length > MAX_NAME_LENGTH) {
            errors += "Invalid pack name"
        }
        if (manifest.author != null && manifest.author.length > MAX_AUTHOR_LENGTH) {
            errors += "Author is too long"
        }
        if (manifest.canvas.width !in 1..MAX_CANVAS_SIDE ||
            manifest.canvas.height !in 1..MAX_CANVAS_SIDE
        ) {
            errors += "Canvas dimensions are out of range"
        }
        if (!manifest.canvas.defaultScale.isFinite() ||
            manifest.canvas.defaultScale !in MIN_DEFAULT_SCALE..MAX_DEFAULT_SCALE
        ) {
            errors += "Canvas defaultScale is out of range"
        }
        if (!manifest.anchor.x.isFinite() || manifest.anchor.x !in 0f..1f ||
            !manifest.anchor.y.isFinite() || manifest.anchor.y !in 0f..1f
        ) {
            errors += "Anchor must be normalized"
        }
        manifest.speechAnchor?.let { speechAnchor ->
            if (!speechAnchor.x.isFinite() || speechAnchor.x !in 0f..1f ||
                !speechAnchor.y.isFinite() || speechAnchor.y !in 0f..1f
            ) {
                errors += "Speech anchor must be normalized"
            }
        }
        if (manifest.clips.size !in 1..MAX_CLIPS) errors += "Invalid clip count"
        val idle = manifest.clips[PetAction.IDLE]
        if (idle == null || !idle.loops) errors += "A looping idle clip is required"
        if (manifest.clips[PetAction.WALK] == null) errors += "A walk clip is required"
        if (manifest.interaction.tapAction !in manifest.clips) {
            errors += "Tap action does not reference a clip"
        }

        var totalFrames = 0
        val referencedFiles = mutableSetOf<String>()
        manifest.clips.forEach { (action, clip) ->
            if (clip.action != action) errors += "Clip key does not match action $action"
            if (clip.frames.isEmpty()) errors += "$action clip has no frames"
            if (clip.loops && clip.nextAction != null) {
                errors += "$action looping clip cannot define nextAction"
            }
            if (!clip.loops && clip.nextAction == action) {
                errors += "$action clip cannot transition to itself"
            }
            if (clip.nextAction != null && clip.nextAction !in manifest.clips) {
                errors += "$action nextAction does not reference a clip"
            }
            totalFrames += clip.frames.size
            clip.frames.forEachIndexed { index, frame ->
                val label = "$action frame $index"
                if (!isSafeAssetPath(frame.file)) errors += "$label has an unsafe file path"
                if (frame.durationMillis !in MIN_FRAME_DURATION..MAX_FRAME_DURATION) {
                    errors += "$label duration is out of range"
                }
                if (!frame.velocity.x.isFinite() || !frame.velocity.y.isFinite() ||
                    frame.velocity.x !in -MAX_VELOCITY..MAX_VELOCITY ||
                    frame.velocity.y !in -MAX_VELOCITY..MAX_VELOCITY
                ) {
                    errors += "$label velocity is out of range"
                }
                val image = images[frame.file]
                if (image == null) {
                    errors += "$label image is missing"
                } else {
                    referencedFiles += frame.file
                    validateRect(label, frame.rect, image, errors)
                }
            }
        }
        if (totalFrames !in 1..MAX_TOTAL_FRAMES) errors += "Too many animation frames"
        if (referencedFiles.sumOf { images.getValue(it).byteCount } > MAX_REFERENCED_BYTES) {
            errors += "Referenced images exceed the decoded asset budget"
        }
        return PetPackValidationResult(errors.distinct())
    }

    fun isSafeAssetPath(path: String): Boolean {
        if (path.isBlank() || path.length > MAX_PATH_LENGTH) return false
        if (path.startsWith('/') || path.startsWith('\\') || '\\' in path || '\u0000' in path) {
            return false
        }
        val segments = path.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) return false
        return path.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase() in ALLOWED_IMAGE_EXTENSIONS
    }

    private fun validateRect(
        label: String,
        rect: PetPackFrameRect,
        image: PetImageInfo,
        errors: MutableList<String>
    ) {
        val right = rect.x.toLong() + rect.width
        val bottom = rect.y.toLong() + rect.height
        if (rect.x < 0 || rect.y < 0 || rect.width <= 0 || rect.height <= 0 ||
            right > image.width || bottom > image.height
        ) {
            errors += "$label rectangle is outside its image"
        }
    }

    companion object {
        val PACK_ID = Regex("[a-z0-9][a-z0-9._-]{0,63}")
        val ALLOWED_IMAGE_EXTENSIONS = setOf("png", "webp")
        const val MAX_PACK_VERSION = 1_000_000
        const val MAX_NAME_LENGTH = 80
        const val MAX_AUTHOR_LENGTH = 80
        const val MAX_CANVAS_SIDE = 2_048
        const val MIN_DEFAULT_SCALE = 0.25f
        const val MAX_DEFAULT_SCALE = 4f
        const val MAX_CLIPS = 24
        const val MAX_TOTAL_FRAMES = 256
        const val MIN_FRAME_DURATION = 16L
        const val MAX_FRAME_DURATION = 10_000L
        const val MAX_VELOCITY = 2_000f
        const val MAX_PATH_LENGTH = 160
        const val MAX_REFERENCED_BYTES = 64L * 1024L * 1024L
    }
}
