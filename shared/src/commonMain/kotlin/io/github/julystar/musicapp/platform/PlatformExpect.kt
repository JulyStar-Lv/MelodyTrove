package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable
import io.github.julystar.musicapp.core.domain.model.AppLanguageMode
import io.github.julystar.musicapp.core.domain.repository.DiagnosticExportPresenter

expect fun getAppDataDirectory(): String
expect fun getAppCacheDir(): String
expect fun getAppDatabasePath(): String?
expect fun getPlatformName(): String
expect fun getProcessName(): String
expect fun platformSettingsCapabilities(): io.github.julystar.musicapp.core.domain.model.SettingsCapabilities
expect fun applyAppLanguageMode(mode: AppLanguageMode)
expect fun currentTimeMillis(): Long
expect fun diagnosticExportPresenter(): DiagnosticExportPresenter
expect fun byteArrayToImageBitmap(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap?

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
