package com.github.tidetunes.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
internal actual fun postOnMainThread(f: () -> Unit) {
    dispatch_async(dispatch_get_main_queue(), f)
}
