package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationType

internal object BatteryAnimationPreviewPolicy {
    fun requiresLocalFile(animation: BatteryAnimationEntry): Boolean =
        animation.type == BatteryAnimationType.LOTTIE &&
            (animation.assetPath.startsWith("https://") ||
                animation.assetPath.startsWith("http://"))

    fun applyLocalFiles(
        animations: List<BatteryAnimationEntry>,
        localFiles: Map<String, String>
    ): List<BatteryAnimationEntry> = animations.map { animation ->
        localFiles[animation.assetPath]?.let { localPath ->
            animation.copy(assetPath = localPath)
        } ?: animation
    }
}
