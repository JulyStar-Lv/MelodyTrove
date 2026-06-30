package com.github.tidetunes.core.presentation.overlay

import androidx.lifecycle.ViewModel
import com.github.tidetunes.core.domain.repository.ToastRepository

class ToastVM(
    toastRepository: ToastRepository,
) : ViewModel() {
    val toast = toastRepository.toast
    val toastRes = toastRepository.toastRes
}
