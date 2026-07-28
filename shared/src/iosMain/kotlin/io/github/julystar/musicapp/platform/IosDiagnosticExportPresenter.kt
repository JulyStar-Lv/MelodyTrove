package io.github.julystar.musicapp.platform

import io.github.julystar.musicapp.core.domain.repository.DiagnosticExportPresenter
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIPasteboard

actual fun diagnosticExportPresenter(): DiagnosticExportPresenter = IosDiagnosticExportPresenter

private object IosDiagnosticExportPresenter : DiagnosticExportPresenter {
    override suspend fun share(path: String): Result<Unit> = runCatching {
        val controller = topViewController()
            ?: error("No active iOS view controller is available")
        val url = NSURL.fileURLWithPath(path)
        controller.presentViewController(
            UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            ),
            animated = true,
            completion = null,
        )
    }

    override suspend fun saveAs(path: String): Result<Unit> = share(path)

    override suspend fun reveal(path: String): Result<Unit> = share(path)

    override suspend fun copyPath(path: String): Result<Unit> = runCatching {
        UIPasteboard.generalPasteboard.string = path
    }

    private fun topViewController(): UIViewController? {
        var current = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (current?.presentedViewController != null) {
            current = current.presentedViewController
        }
        return current
    }
}
