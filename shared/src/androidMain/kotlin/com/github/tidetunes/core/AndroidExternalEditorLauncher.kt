package com.github.tidetunes.core

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.github.tidetunes.core.domain.model.LyricTimingEditorApp
import com.github.tidetunes.core.domain.model.MetadataEditorApp
import com.github.tidetunes.core.domain.repository.ExternalEditorKind
import com.github.tidetunes.core.domain.repository.ExternalEditorLauncher
import com.github.tidetunes.core.domain.repository.ExternalEditorRequest
import java.io.File

class AndroidExternalEditorLauncher(
    private val context: Context,
) : ExternalEditorLauncher {
    override val isSupported: Boolean = true

    override fun launch(request: ExternalEditorRequest): Boolean {
        val uri = request.editableUri() ?: return false
        val intents = when (request.kind) {
            ExternalEditorKind.Metadata -> metadataIntents(request, uri)
            ExternalEditorKind.LyricTiming -> lyricTimingIntents(request, uri)
        }
        return intents.any { intent ->
            runCatching {
                context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }.isSuccess
        }
    }

    private fun metadataIntents(request: ExternalEditorRequest, uri: Uri): List<Intent> =
        when (request.metadataEditor) {
            MetadataEditorApp.AskEveryTime -> listOf(
                Intent.createChooser(baseEditIntent(request, uri), "Choose metadata editor")
            )
            MetadataEditorApp.Lyrico -> listOf(
                baseEditIntent(request, uri).apply {
                    action = "com.lonx.lyrico.action.EDIT_TAG"
                    setPackage("com.lonx.lyrico")
                }
            )
            MetadataEditorApp.LunaBeat -> lunaBeatIntents(
                request = request,
                uri = uri,
                activity = "com.example.LyricBox.SongMetadataEditActivity",
            )
            MetadataEditorApp.MusicTag -> listOf(
                baseEditIntent(request, uri).apply {
                    component = ComponentName(
                        "com.xjcheng.musictageditor",
                        "com.xjcheng.musictageditor.SongDetailActivity",
                    )
                    putExtra("filepath", request.sourcePath)
                },
                baseEditIntent(request, uri).apply {
                    action = Intent.ACTION_VIEW
                    setPackage("com.xjcheng.musictageditor")
                },
            )
        }

    private fun lyricTimingIntents(request: ExternalEditorRequest, uri: Uri): List<Intent> =
        when (request.lyricTimingEditor) {
            LyricTimingEditorApp.AskEveryTime -> listOf(
                Intent.createChooser(
                    baseEditIntent(request, uri).apply { action = Intent.ACTION_SEND },
                    "Choose lyric timing editor",
                )
            )
            LyricTimingEditorApp.LunaBeat -> lunaBeatIntents(
                request = request,
                uri = uri,
                activity = "com.example.LyricBox.LyricTimingActivity",
            )
        }

    private fun lunaBeatIntents(
        request: ExternalEditorRequest,
        uri: Uri,
        activity: String,
    ): List<Intent> = listOf("com.example.LyricBox", "com.example.lyricbox").map { packageName ->
        baseEditIntent(request, uri).apply {
            component = ComponentName(packageName, activity)
            putExtra("audioPath", request.sourcePath)
            putExtra("audio_path", request.sourcePath)
            putExtra("source_audio_path", request.sourcePath)
        }
    }

    private fun baseEditIntent(request: ExternalEditorRequest, uri: Uri): Intent =
        Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, "audio/*")
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, request.title)
            putExtra("title", request.title)
            putExtra("artist", request.artist.orEmpty())
            putExtra("path", request.sourcePath)
            putExtra("filePath", request.sourcePath)
            putExtra("id", request.trackId)
            putExtra("songId", request.trackId)
            putExtra("mediaId", request.trackId)
            putExtra("uri", uri.toString())
            putExtra("contentUri", uri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, request.title, uri)
        }

    private fun ExternalEditorRequest.editableUri(): Uri? {
        if (sourcePath.startsWith("content://")) return Uri.parse(sourcePath)
        val file = File(sourcePath.removePrefix("file://"))
        if (!file.isFile) return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }
}
