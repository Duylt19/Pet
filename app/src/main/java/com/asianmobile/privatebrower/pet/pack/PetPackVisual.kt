package com.asianmobile.privatebrower.pet.pack

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.LruCache
import com.asianmobile.privatebrower.pet.engine.PetAction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PetPackVisual {
    data object CodeNative : PetPackVisual
    data class Sprite(
        val frames: Map<PetAction, List<PetSpriteFrame>>,
        val canvas: PetPackCanvas,
        val anchor: PetPackAnchor
    ) : PetPackVisual
}

data class PetSpriteFrame(
    val bitmap: Bitmap,
    val source: Rect
)

@Singleton
class PetBitmapCache @Inject constructor(
    @param:ApplicationContext context: Context
) {
    private val cache = object : LruCache<String, Bitmap>(cacheBudgetKilobytes(context)) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / BYTES_PER_KILOBYTE).coerceAtLeast(1)
    }

    fun prepare(pack: PetPack): PetPackVisual {
        val source = pack.source as? PetPackSource.Installed ?: return PetPackVisual.CodeNative
        return runCatching {
            val root = File(source.directoryPath)
            val bitmaps = pack.manifest.clips.values
                .flatMap(PetPackClip::frames)
                .map(PetPackFrame::file)
                .toSet()
                .associateWith { relativePath ->
                    cache.get("${pack.key}:$relativePath") ?: decode(root, relativePath).also {
                        cache.put("${pack.key}:$relativePath", it)
                    }
                }
            val frames = pack.manifest.clips.mapValues { (_, clip) ->
                clip.frames.map { frame ->
                    val rect = frame.rect
                    PetSpriteFrame(
                        bitmap = bitmaps.getValue(frame.file),
                        source = Rect(
                            rect.x,
                            rect.y,
                            rect.x + rect.width,
                            rect.y + rect.height
                        )
                    )
                }
            }.normalizedRuntimeVisualFrames(pack.manifest.id)
            PetPackVisual.Sprite(
                frames = frames,
                canvas = pack.manifest.canvas,
                anchor = pack.manifest.anchor
            )
        }.getOrDefault(PetPackVisual.CodeNative)
    }

    private fun decode(root: File, relativePath: String): Bitmap {
        val file = File(root, relativePath)
        return checkNotNull(BitmapFactory.decodeFile(file.absolutePath)) {
            "Unable to decode pet frame"
        }
    }

    private companion object {
        const val BYTES_PER_KILOBYTE = 1024
        const val MIN_CACHE_KILOBYTES = 4 * 1024
        const val MAX_CACHE_KILOBYTES = 24 * 1024

        fun cacheBudgetKilobytes(context: Context): Int {
            val manager = context.getSystemService(ActivityManager::class.java)
            val appBudgetKilobytes = manager.memoryClass * 1024
            return (appBudgetKilobytes / 16).coerceIn(
                MIN_CACHE_KILOBYTES,
                MAX_CACHE_KILOBYTES
            )
        }
    }
}
