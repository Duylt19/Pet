package com.asianmobile.emojibattery.shimeji.pet.pack

import kotlin.math.ceil
import kotlin.math.roundToInt

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

        val leftEdges = List(height) { y ->
            (0 until width).firstOrNull { x -> isOpaque(x, y) }
        }
        val globalLeftEdge = leftEdges.filterNotNull().minOrNull() ?: return null
        resolveNotch(width, height, leftEdges)?.let { return it }

        val edgeBandWidth = maxOf(
            MIN_EDGE_BAND_PIXELS,
            ceil(width * EDGE_BAND_FRACTION).toInt()
        )
        val edgeRight = (globalLeftEdge + edgeBandWidth).coerceAtMost(width)
        val opaqueRows = buildList {
            for (x in globalLeftEdge until edgeRight) {
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
            x = FALLBACK_X,
            y = (medianY / height.toFloat()).coerceIn(MIN_Y, MAX_Y)
        )
    }

    private fun resolveNotch(
        width: Int,
        height: Int,
        leftEdges: List<Int?>
    ): PetPackAnchor? {
        val stableWindow = maxOf(
            MIN_STABLE_WINDOW_ROWS,
            (height * STABLE_WINDOW_FRACTION).roundToInt()
        )
        val minimumDrop = maxOf(
            MIN_NOTCH_DROP_PIXELS,
            (width * MIN_NOTCH_DROP_FRACTION).roundToInt()
        )
        val maximumSpread = maxOf(
            MIN_STABLE_SPREAD_PIXELS,
            (width * MAX_STABLE_SPREAD_FRACTION).roundToInt()
        )
        val firstRow = maxOf(stableWindow, (height * MIN_NOTCH_Y).roundToInt())
        val lastRow = minOf(height, (height * MAX_NOTCH_Y).roundToInt())
        val minimumBaselineX = width * MIN_NOTCH_X
        val maximumBaselineX = width * MAX_NOTCH_X

        return (firstRow until lastRow)
            .mapNotNull { y ->
                val current = leftEdges[y] ?: return@mapNotNull null
                val previous = leftEdges.subList(y - stableWindow, y).filterNotNull()
                if (previous.size != stableWindow) return@mapNotNull null
                val baseline = previous.median()
                val spread = previous.max() - previous.min()
                val drop = baseline - current
                if (baseline !in minimumBaselineX..maximumBaselineX ||
                    spread > maximumSpread ||
                    drop < minimumDrop
                ) {
                    return@mapNotNull null
                }
                NotchCandidate(
                    x = baseline,
                    y = y,
                    score = drop - spread * STABILITY_SCORE_WEIGHT
                )
            }
            .maxByOrNull(NotchCandidate::score)
            ?.let { candidate ->
                PetPackAnchor(
                    x = candidate.x / width,
                    y = candidate.y / height.toFloat()
                )
            }
    }

    private fun List<Int>.median(): Float {
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2f
        } else {
            sorted[middle].toFloat()
        }
    }

    private data class NotchCandidate(
        val x: Float,
        val y: Int,
        val score: Float
    )

    private const val EDGE_BAND_FRACTION = 0.02f
    private const val MIN_EDGE_BAND_PIXELS = 2
    private const val FALLBACK_X = 0.5f
    private const val MIN_Y = 0.25f
    private const val MAX_Y = 0.85f
    private const val ALPHA_THRESHOLD = 16
    private const val STABLE_WINDOW_FRACTION = 0.03f
    private const val MIN_STABLE_WINDOW_ROWS = 4
    private const val MIN_NOTCH_DROP_FRACTION = 0.08f
    private const val MIN_NOTCH_DROP_PIXELS = 6
    private const val MAX_STABLE_SPREAD_FRACTION = 0.03f
    private const val MIN_STABLE_SPREAD_PIXELS = 2
    private const val MIN_NOTCH_X = 0.4f
    private const val MAX_NOTCH_X = 0.55f
    private const val MIN_NOTCH_Y = 0.3f
    private const val MAX_NOTCH_Y = 0.81f
    private const val STABILITY_SCORE_WEIGHT = 0.5f
}
