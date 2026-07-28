package com.github.tidetunes.diagnostics

import android.content.pm.ApplicationInfo
import com.github.tidetunes.platform.appContext

internal actual fun platformDebugFaultInjectionSupported(): Boolean =
    appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

internal actual fun triggerPlatformKotlinCrash() {
    Thread {
        error("TideTunes debug Kotlin uncaught exception fault injection")
    }.apply { name = "diagnostics-kotlin-crash" }.start()
}

internal actual fun triggerPlatformAnr() {
    check(platformDebugFaultInjectionSupported()) { "ANR injection is disabled" }
    Thread.sleep(30_000)
}
