package io.github.julystar.musicapp.feature.settings.presentation

import android.content.Intent
import android.provider.DocumentsContract
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context
import androidx.core.net.toUri

internal actual fun normalizePickedDirectoryPath(path: String): String? {
    if (!path.startsWith("content://", ignoreCase = true)) return path

    val uri = path.toUri()
    val permissionFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    val permissionGranted = runCatching {
        FileKit.context.contentResolver.takePersistableUriPermission(uri, permissionFlags)
    }.recoverCatching {
        FileKit.context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.isSuccess
    if (!permissionGranted) return null

    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return null
    if (treeDocumentId.substringBefore(':') != "primary") return null

    val relativePath = treeDocumentId.substringAfter(':', missingDelimiterValue = "").trim('/')
    // LocalBackend adds /storage/emulated/0 before opening the selected folder.
    return if (relativePath.isEmpty()) "/" else "/$relativePath"
}
