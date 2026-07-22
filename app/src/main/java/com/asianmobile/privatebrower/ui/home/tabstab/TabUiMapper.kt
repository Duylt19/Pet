package com.asianmobile.privatebrower.ui.home.tabstab

import android.content.Context
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.browser.TabPreview
import com.asianmobile.privatebrower.data.browser.TabSession
import java.io.File

internal fun TabSession.toTabUi(
    context: Context,
    activeId: Long?,
    preview: TabPreview?
): TabUi {
    val thumbnailDirectory = File(context.cacheDir, "tabs")
    val jpegThumbnail = File(thumbnailDirectory, "$id.jpg")
    val legacyPngThumbnail = File(thumbnailDirectory, "$id.png")
    val thumbnailFile = jpegThumbnail.takeIf { it.exists() && it.length() > 0L }
        ?: legacyPngThumbnail.takeIf { it.exists() && it.length() > 0L }
    return TabUi(
        id = id,
        title = title.value.ifBlank { url.value.ifBlank { context.getString(R.string.tab_plus) } },
        url = url.value,
        thumbnailPath = thumbnailFile?.absolutePath,
        isActive = id == activeId,
        thumbnailTimestamp = preview?.revision ?: thumbnailFile?.lastModified() ?: 0L,
        thumbnailBitmap = preview?.bitmap
    )
}
