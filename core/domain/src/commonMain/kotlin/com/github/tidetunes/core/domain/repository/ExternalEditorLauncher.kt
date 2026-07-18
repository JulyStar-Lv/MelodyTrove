package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.LyricTimingEditorApp
import com.github.tidetunes.core.domain.model.MetadataEditorApp

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
