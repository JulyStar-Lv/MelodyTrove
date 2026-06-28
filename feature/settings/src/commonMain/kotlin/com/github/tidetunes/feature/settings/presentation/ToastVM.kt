package com.github.tidetunes.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.github.tidetunes.core.domain.repository.ToastRepository


class ToastVM constructor(
    private val toastRepository: ToastRepository
) : ViewModel() {
    val toast = toastRepository.toast
    val toastRes = toastRepository.toastRes
}
