package com.github.tidetunes.platform

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.repository.DiagnosticExportPresenter

expect fun getAppDocumentDir(): String
expect fun getAppCacheDir(): String
expect fun getAppDatabasePath(): String?
expect fun getPlatformName(): String
expect fun getProcessName(): String
expect fun isSystemDynamicColorAvailable(): Boolean
expect fun platformSettingsCapabilities(): com.github.tidetunes.core.domain.model.SettingsCapabilities
expect fun applyAppLanguageMode(mode: AppLanguageMode)
expect fun currentTimeMillis(): Long
expect fun diagnosticExportPresenter(): DiagnosticExportPresenter
expect fun byteArrayToImageBitmap(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap?

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
