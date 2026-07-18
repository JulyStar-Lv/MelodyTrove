package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.domain.repository.ExternalEditorLauncher
import com.github.tidetunes.core.domain.repository.ExternalEditorRequest

class UnsupportedExternalEditorLauncher : ExternalEditorLauncher {
    override val isSupported: Boolean = false
    override fun launch(request: ExternalEditorRequest): Boolean = false
}
