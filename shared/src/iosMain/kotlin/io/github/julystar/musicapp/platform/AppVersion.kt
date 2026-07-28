package io.github.julystar.musicapp.platform

actual fun getAppVersion(): String {
    return GeneratedBuildInfo.appVersionName
}

actual fun getAppBuildInfo(): String {
    return "iOS bundle"
}
