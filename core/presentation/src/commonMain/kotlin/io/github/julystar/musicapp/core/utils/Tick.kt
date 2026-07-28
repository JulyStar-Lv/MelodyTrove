package io.github.julystar.musicapp.core.utils

internal expect fun postOnMainThread(f: () -> Unit)

fun nextTickOnMain(f: () -> Unit) {
    postOnMainThread { f() }
}