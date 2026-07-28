package com.github.tidetunes.diagnostics

internal actual fun platformDebugFaultInjectionSupported(): Boolean =
    System.getProperty("tidetunes.developerMode").toBoolean() ||
        System.getenv("TIDETUNES_DEVELOPER_MODE").toBoolean()

internal actual fun triggerPlatformKotlinCrash() {
    Thread {
        error("TideTunes developer-mode Kotlin uncaught exception fault injection")
    }.apply { name = "diagnostics-kotlin-crash" }.start()
}

internal actual fun triggerPlatformAnr() {
    error("Android ANR injection is only available on Android")
}
