package io.github.julystar.musicapp.core.utils

import java.awt.EventQueue

internal actual fun postOnMainThread(f: () -> Unit) {
    EventQueue.invokeLater { f() }
}
