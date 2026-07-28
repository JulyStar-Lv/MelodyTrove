package io.github.julystar.musicapp.core.domain.repository

import kotlinx.coroutines.flow.SharedFlow

interface ToastRepository {
    val toast: SharedFlow<String>
    val toastRes: SharedFlow<Int>
    fun emitToast(msg: String)
    fun emitToastRes(resId: Int)
}
