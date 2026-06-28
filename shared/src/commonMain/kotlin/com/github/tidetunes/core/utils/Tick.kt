package com.github.tidetunes.core.utils

internal expect fun postOnMainThread(f: () -> Unit)

fun nextTickOnMain(f: () -> Unit) {
    postOnMainThread { f() }
}