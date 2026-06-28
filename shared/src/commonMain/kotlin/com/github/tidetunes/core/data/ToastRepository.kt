package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.repository.ToastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ToastRepositoryImpl(
    private val scope: CoroutineScope
) : ToastRepository {
    private val _toast = MutableSharedFlow<String>()
    private val _toastRes = MutableSharedFlow<Int>()

    override val toast = _toast.asSharedFlow()
    override val toastRes = _toastRes.asSharedFlow()

    override fun emitToast(msg: String) {
        scope.launch {
            _toast.emit(msg)
        }
    }

    override fun emitToastRes(resId: Int) {
        scope.launch {
            _toastRes.emit(resId)
        }
    }
}
