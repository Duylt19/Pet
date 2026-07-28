package com.asianmobile.emojibattery.shimeji.pet.pack

import kotlin.math.ceil

internal object LegacySpeechAnchorResolver {
    fun resolveArgb(
        width: Int,
        height: Int,
        argbAt: (x: Int, y: Int) -> Int
    ): PetPackAnchor? = resolve(width, height) { x, y ->
        argbAt(x, y).ushr(24) >= ALPHA_THRESHOLD
    }

    fun resolve(
        width: Int,
        height: Int,
        isOpaque: (x: Int, y: Int) -> Boolean
    ): PetPackAnchor? {
        require(width > 0 && height > 0) { "image size must be positive" }

        val leftEdge = (0 until width).firstOrNull { x ->
            (0 until height).any { y -> isOpaque(x, y) }
        } ?: return null
        val edgeBandWidth = maxOf(MIN_EDGE_BAND_PIXELS, ceil(width * EDGE_BAND_FRACTION).toInt())
        val edgeRight = (leftEdge + edgeBandWidth).coerceAtMost(width)
        val opaqueRows = buildList {
            for (x in leftEdge until edgeRight) {
                for (y in 0 until height) {
                    if (isOpaque(x, y)) add(y)
                }
            }
        }.sorted()
        if (opaqueRows.isEmpty()) return null

        val medianY = if (opaqueRows.size % 2 == 0) {
            val upper = opaqueRows.size / 2
            (opaqueRows[upper - 1] + opaqueRows[upper]) / 2f
        } else {
            opaqueRows[opaqueRows.size / 2].toFloat()
        }
        return PetPackAnchor(
            x = (leftEdge / width.toFloat()).coerceIn(MIN_X, MAX_X),
            y = (medianY / height.toFloat()).coerceIn(MIN_Y, MAX_Y)
        )
    }

    private const val EDGE_BAND_FRACTION = 0.02f
    private const val MIN_EDGE_BAND_PIXELS = 2
    private const val MIN_X = 0.05f
    private const val MAX_X = 0.45f
    private const val MIN_Y = 0.25f
    private const val MAX_Y = 0.85f
    private const val ALPHA_THRESHOLD = 16
}
