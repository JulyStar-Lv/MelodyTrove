package io.github.julystar.musicapp.core.data.settings

import io.github.julystar.musicapp.core.domain.repository.ExternalEditorLauncher
import io.github.julystar.musicapp.core.domain.repository.ExternalEditorRequest

class UnsupportedExternalEditorLauncher : ExternalEditorLauncher {
    override val isSupported: Boolean = false
    override fun launch(request: ExternalEditorRequest): Boolean = false
}
