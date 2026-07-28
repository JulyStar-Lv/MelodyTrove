package com.github.tidetunes.diagnostics

internal expect fun platformDebugFaultInjectionSupported(): Boolean
internal expect fun triggerPlatformKotlinCrash()
internal expect fun triggerPlatformAnr()
