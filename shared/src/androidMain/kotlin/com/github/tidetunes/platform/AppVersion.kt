package com.github.tidetunes.platform

import android.content.pm.PackageManager
import android.os.Build

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

actual fun getAppBuildInfo(): String {
    val buildType = if (
        appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    ) {
        "Debug"
    } else {
        "Release"
    }
    return "$buildType, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
}
