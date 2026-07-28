package com.github.tidetunes.platform

import com.github.tidetunes.core.domain.repository.DiagnosticExportPresenter
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun diagnosticExportPresenter(): DiagnosticExportPresenter = DesktopDiagnosticExportPresenter

private object DesktopDiagnosticExportPresenter : DiagnosticExportPresenter {
    override suspend fun share(path: String): Result<Unit> = runCatching {
        Desktop.getDesktop().open(File(path))
    }

    override suspend fun saveAs(path: String): Result<Unit> = runCatching {
        val source = File(path)
        val dialog = FileDialog(null as Frame?, "Save diagnostics", FileDialog.SAVE).apply {
            file = source.name
            isVisible = true
        }
        val directory = dialog.directory ?: return@runCatching
        val fileName = dialog.file ?: return@runCatching
        Files.copy(
            source.toPath(),
            File(directory, fileName).toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    override suspend fun reveal(path: String): Result<Unit> = runCatching {
        val file = File(path)
        val os = System.getProperty("os.name").orEmpty().lowercase()
        when {
            os.contains("mac") -> ProcessBuilder("open", "-R", file.absolutePath).start()
            os.contains("win") -> ProcessBuilder("explorer", "/select,${file.absolutePath}").start()
            else -> Desktop.getDesktop().open(file.parentFile)
        }
    }

    override suspend fun copyPath(path: String): Result<Unit> = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(path), null)
    }
}
