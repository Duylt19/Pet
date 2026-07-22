package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.io.File
import java.util.Locale

internal enum class FileTypeKind {
    VIDEO,
    IMAGE,
    AUDIO,
    PDF,
    DOC,
    XLS,
    PPT,
    TXT,
    ZIP,
    RAR,
    EXE,
    APK,
    CODE,
    EBOOK,
    OTHER
}

@Composable
internal fun FileTypeIcon(
    fileName: String,
    filePath: String,
    mimeType: String,
    modifier: Modifier = Modifier
) {
    val extension = remember(fileName, filePath) {
        sequenceOf(fileName, filePath)
            .map { it.substringBefore('?').substringBefore('#') }
            .map { File(it).extension.lowercase(Locale.ROOT) }
            .firstOrNull(String::isNotBlank)
            .orEmpty()
    }
    val kind = remember(mimeType, extension) {
        fileTypeKind(mimeType = mimeType, extension = extension)
    }
    FileTypeIcon(kind = kind, extension = extension, modifier = modifier)
}

@Composable
internal fun FileTypeIcon(
    kind: FileTypeKind,
    extension: String,
    modifier: Modifier = Modifier
) {
    val accentColor = colorResource(fileTypeAccentColor(kind))
    val fallbackLabel = stringResource(fileTypeLabel(kind))
    val label = if (kind == FileTypeKind.OTHER) {
        extension.uppercase(Locale.ROOT).take(4).ifBlank { fallbackLabel }
    } else {
        fallbackLabel
    }
    val badgeWidth = if (label.length > 3) {
        dimensionResource(SdpR.dimen._18sdp)
    } else {
        dimensionResource(SdpR.dimen._14sdp)
    }
    val labelSize = if (label.length > 3) SspR.dimen._3ssp else SspR.dimen._4ssp

    Box(modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_download_file_outline),
            contentDescription = null,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = dimensionResource(SdpR.dimen._2sdp),
                    y = dimensionResource(SdpR.dimen._14sdp)
                )
                .width(badgeWidth)
                .height(dimensionResource(SdpR.dimen._9sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._4sdp)))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = with(LocalDensity.current) {
                    dimensionResource(labelSize).toSp()
                },
                color = colorResource(R.color.colors_FFFFFF),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun fileTypeAccentColor(kind: FileTypeKind): Int = when (kind) {
    FileTypeKind.VIDEO -> R.color.colors_3369FD
    FileTypeKind.IMAGE -> R.color.colors_22C55E
    FileTypeKind.AUDIO,
    FileTypeKind.PPT,
    FileTypeKind.EBOOK -> R.color.colors_D99335
    FileTypeKind.PDF -> R.color.colors_F7343F
    FileTypeKind.DOC,
    FileTypeKind.TXT,
    FileTypeKind.CODE -> R.color.colors_348FF7
    FileTypeKind.XLS,
    FileTypeKind.EXE,
    FileTypeKind.APK -> R.color.colors_2FBE49
    FileTypeKind.ZIP,
    FileTypeKind.RAR -> R.color.colors_F2D232
    FileTypeKind.OTHER -> R.color.colors_6F7073
}

private fun fileTypeLabel(kind: FileTypeKind): Int = when (kind) {
    FileTypeKind.VIDEO -> R.string.file_type_video
    FileTypeKind.IMAGE -> R.string.file_type_image
    FileTypeKind.AUDIO -> R.string.file_type_audio
    FileTypeKind.PDF -> R.string.file_type_pdf
    FileTypeKind.DOC -> R.string.file_type_document
    FileTypeKind.XLS -> R.string.file_type_spreadsheet
    FileTypeKind.PPT -> R.string.file_type_presentation
    FileTypeKind.TXT -> R.string.file_type_text
    FileTypeKind.ZIP -> R.string.file_type_archive
    FileTypeKind.RAR -> R.string.file_type_rar
    FileTypeKind.EXE -> R.string.file_type_executable
    FileTypeKind.APK -> R.string.file_type_apk
    FileTypeKind.CODE -> R.string.file_type_code
    FileTypeKind.EBOOK -> R.string.file_type_ebook
    FileTypeKind.OTHER -> R.string.file_type_file
}

private val videoExtensions = setOf(
    "mp4", "m4v", "mkv", "webm", "mov", "avi", "ts", "flv", "3gp", "3g2", "mpeg", "mpg"
)
private val imageExtensions = setOf(
    "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "svg", "tif", "tiff"
)
private val audioExtensions = setOf(
    "mp3", "m4a", "aac", "wav", "ogg", "oga", "flac", "opus", "wma", "amr", "aiff", "mid", "midi"
)
private val documentExtensions = setOf("doc", "docx", "dot", "dotx", "rtf", "odt", "pages")
private val spreadsheetExtensions = setOf("xls", "xlsx", "xlsm", "csv", "ods", "numbers")
private val presentationExtensions = setOf("ppt", "pptx", "pps", "ppsx", "odp", "key")
private val textExtensions = setOf("txt", "log", "m3u", "m3u8", "ini", "conf")
private val archiveExtensions = setOf(
    "zip", "7z", "tar", "gz", "bz2", "xz", "tgz", "zst", "lz", "lz4", "cab", "jar"
)
private val executableExtensions = setOf(
    "exe", "msi", "msix", "bat", "cmd", "com", "deb", "rpm", "appimage"
)
private val androidPackageExtensions = setOf("apk", "apks", "xapk", "aab")
private val ebookExtensions = setOf("epub", "mobi", "azw", "azw3", "fb2")
private val codeExtensions = setOf(
    "html", "htm", "css", "scss", "sass", "less", "js", "mjs", "cjs", "ts", "tsx", "jsx",
    "json", "xml", "yaml", "yml", "toml", "md", "kt", "kts", "java", "py", "rb", "php",
    "swift", "c", "cc", "cpp", "h", "hpp", "cs", "go", "rs", "sh", "bash", "zsh", "sql", "gradle"
)

internal fun fileTypeKind(mimeType: String, extension: String): FileTypeKind {
    val normalizedMime = mimeType.substringBefore(';').trim().lowercase()
    val normalizedExtension = extension.trim().removePrefix(".").lowercase()

    return when {
        normalizedMime.startsWith("video/") || normalizedExtension in videoExtensions -> FileTypeKind.VIDEO
        normalizedMime.startsWith("image/") || normalizedExtension in imageExtensions -> FileTypeKind.IMAGE
        normalizedMime.startsWith("audio/") || normalizedExtension in audioExtensions -> FileTypeKind.AUDIO
        normalizedExtension == "pdf" -> FileTypeKind.PDF
        normalizedExtension in androidPackageExtensions -> FileTypeKind.APK
        normalizedExtension in documentExtensions -> FileTypeKind.DOC
        normalizedExtension in spreadsheetExtensions -> FileTypeKind.XLS
        normalizedExtension in presentationExtensions -> FileTypeKind.PPT
        normalizedExtension == "rar" -> FileTypeKind.RAR
        normalizedExtension in archiveExtensions -> FileTypeKind.ZIP
        normalizedExtension in executableExtensions -> FileTypeKind.EXE
        normalizedExtension in ebookExtensions -> FileTypeKind.EBOOK
        normalizedExtension in codeExtensions -> FileTypeKind.CODE
        normalizedExtension in textExtensions -> FileTypeKind.TXT
        normalizedMime == "application/pdf" -> FileTypeKind.PDF
        normalizedMime in androidPackageMimeTypes -> FileTypeKind.APK
        normalizedMime in documentMimeTypes -> FileTypeKind.DOC
        normalizedMime in spreadsheetMimeTypes -> FileTypeKind.XLS
        normalizedMime in presentationMimeTypes -> FileTypeKind.PPT
        normalizedMime in rarMimeTypes -> FileTypeKind.RAR
        normalizedMime in archiveMimeTypes -> FileTypeKind.ZIP
        normalizedMime in executableMimeTypes -> FileTypeKind.EXE
        normalizedMime in ebookMimeTypes -> FileTypeKind.EBOOK
        normalizedMime in codeMimeTypes -> FileTypeKind.CODE
        normalizedMime.startsWith("text/") -> FileTypeKind.TXT
        else -> FileTypeKind.OTHER
    }
}

private val androidPackageMimeTypes = setOf(
    "application/vnd.android.package-archive",
    "application/x-android-package"
)
private val documentMimeTypes = setOf(
    "application/msword",
    "application/rtf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
    "application/vnd.oasis.opendocument.text"
)
private val spreadsheetMimeTypes = setOf(
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
    "application/vnd.oasis.opendocument.spreadsheet",
    "text/csv"
)
private val presentationMimeTypes = setOf(
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
    "application/vnd.oasis.opendocument.presentation"
)
private val rarMimeTypes = setOf("application/vnd.rar", "application/x-rar-compressed")
private val archiveMimeTypes = setOf(
    "application/zip",
    "application/x-7z-compressed",
    "application/x-tar",
    "application/gzip",
    "application/x-gzip",
    "application/x-bzip2",
    "application/x-xz",
    "application/java-archive"
)
private val executableMimeTypes = setOf(
    "application/vnd.microsoft.portable-executable",
    "application/x-msdownload",
    "application/x-ms-installer",
    "application/vnd.debian.binary-package",
    "application/x-rpm"
)
private val ebookMimeTypes = setOf("application/epub+zip", "application/x-mobipocket-ebook")
private val codeMimeTypes = setOf(
    "application/json",
    "application/ld+json",
    "application/xml",
    "application/javascript",
    "application/x-javascript",
    "application/x-httpd-php",
    "application/sql"
)
