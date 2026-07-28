package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.LyricTimingEditorApp
import io.github.julystar.musicapp.core.domain.model.MetadataEditorApp

enum class ExternalEditorKind {
    Metadata,
    LyricTiming,
}

data class ExternalEditorRequest(
    val kind: ExternalEditorKind,
    val trackId: Long,
    val title: String,
    val artist: String?,
    val sourcePath: String,
    val metadataEditor: MetadataEditorApp,
    val lyricTimingEditor: LyricTimingEditorApp,
)

interface ExternalEditorLauncher {
    val isSupported: Boolean
    fun launch(request: ExternalEditorRequest): Boolean
}
