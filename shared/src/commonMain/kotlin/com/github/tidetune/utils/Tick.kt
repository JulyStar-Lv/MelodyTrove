package com.github.tidetune.utils

internal expect fun postOnMainThread(f: () -> Unit)

fun nextTickOnMain(f: () -> Unit) {
    postOnMainThread { f() }
}