package com.github.tidetunes.feature.settings.presentation

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import com.github.tidetunes.platform.appContext

@Composable
actual fun getAppVersion(): String {
    val context = appContext
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
    return packageInfo.versionName ?: "<unknown>"
}
