package com.asianmobile.emojibattery.shimeji.pet.speech

data class PetSpeechBoxSize(
    val width: Int,
    val height: Int
)

data class PetSpeechTextMetrics(
    val usedWidth: Int,
    val height: Int,
    val lineCount: Int
) {
    init {
        require(usedWidth >= 0) { "used text width must not be negative" }
        require(height > 0) { "text height must be positive" }
        require(lineCount > 0) { "text line count must be positive" }
    }
}

data class PetSpeechBoxConstraints(
    val minimumWidth: Int,
    val maximumWidth: Int,
    val widthStep: Int,
    val minimumHeight: Int,
    val maximumHeight: Int,
    val horizontalPadding: Int,
    val verticalPadding: Int,
    val maximumLines: Int,
    val minimumAspectRatio: Float,
    val maximumAspectRatio: Float
) {
    init {
        require(minimumWidth > 0 && maximumWidth >= minimumWidth) {
            "speech width bounds must be valid"
        }
        require(widthStep > 0) { "speech width step must be positive" }
        require(minimumHeight > 0 && maximumHeight >= minimumHeight) {
            "speech height bounds must be valid"
        }
        require(horizontalPadding >= 0 && verticalPadding >= 0) {
            "speech padding must not be negative"
        }
        require(minimumWidth > horizontalPadding * 2) {
            "minimum speech width must leave room for text"
        }
        require(maximumLines > 0) { "maximum speech lines must be positive" }
        require(minimumAspectRatio > 0f) { "minimum speech aspect ratio must be positive" }
        require(maximumAspectRatio >= minimumAspectRatio) {
            "speech aspect ratio bounds must be valid"
        }
    }
}

object PetSpeechBoxSizingPolicy {
    fun resolve(
        constraints: PetSpeechBoxConstraints,
        measureText: (contentWidth: Int) -> PetSpeechTextMetrics
    ): PetSpeechBoxSize {
        val widestMetrics = measureText(constraints.maximumContentWidth)
        if (widestMetrics.lineCount == 1 && widestMetrics.fitsHeight(constraints)) {
            val naturalWidth = (
                widestMetrics.usedWidth +
                    constraints.horizontalPadding * 2 +
                    SINGLE_LINE_ROUNDING_BUFFER
                ).coerceIn(constraints.minimumWidth, constraints.maximumWidth)
            val naturalMetrics = measureText(
                naturalWidth - constraints.horizontalPadding * 2
            )
            if (naturalMetrics.lineCount == 1 && naturalMetrics.fitsHeight(constraints)) {
                val naturalSize = PetSpeechBoxSize(
                    width = naturalWidth,
                    height = naturalMetrics.boxHeight(constraints)
                )
                if (naturalSize.aspectRatio <= constraints.maximumAspectRatio) {
                    return naturalSize
                }
            }
        }

        val candidates = constraints.candidateWidths().mapNotNull { width ->
            val metrics = measureText(width - constraints.horizontalPadding * 2)
            if (metrics.lineCount > constraints.maximumLines ||
                !metrics.fitsHeight(constraints)
            ) {
                null
            } else {
                PetSpeechBoxSize(
                    width = width,
                    height = metrics.boxHeight(constraints)
                )
            }
        }
        return candidates.firstOrNull { size ->
            size.aspectRatio in
                constraints.minimumAspectRatio..constraints.maximumAspectRatio
        } ?: candidates.minByOrNull { size ->
            size.aspectRatio.distanceTo(
                constraints.minimumAspectRatio,
                constraints.maximumAspectRatio
            )
        }
            ?: PetSpeechBoxSize(constraints.maximumWidth, constraints.maximumHeight)
    }

    private fun PetSpeechBoxConstraints.candidateWidths(): List<Int> = buildList {
        var width = minimumWidth
        while (width < maximumWidth) {
            add(width)
            width += widthStep
        }
        add(maximumWidth)
    }

    private fun PetSpeechTextMetrics.fitsHeight(
        constraints: PetSpeechBoxConstraints
    ): Boolean = height + constraints.verticalPadding * 2 <= constraints.maximumHeight

    private fun PetSpeechTextMetrics.boxHeight(
        constraints: PetSpeechBoxConstraints
    ): Int = (height + constraints.verticalPadding * 2)
        .coerceIn(constraints.minimumHeight, constraints.maximumHeight)

    private val PetSpeechBoxConstraints.maximumContentWidth: Int
        get() = maximumWidth - horizontalPadding * 2

    private val PetSpeechBoxSize.aspectRatio: Float
        get() = width.toFloat() / height

    private fun Float.distanceTo(minimum: Float, maximum: Float): Float = when {
        this < minimum -> minimum - this
        this > maximum -> this - maximum
        else -> 0f
    }

    private const val SINGLE_LINE_ROUNDING_BUFFER = 1
}
