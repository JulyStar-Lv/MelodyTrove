package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import com.github.tidetune.singleton.StorageRepository
import com.github.tidetune.singleton.ToastRepository


class ToastVM constructor(
    private val toastRepository: ToastRepository
) : ViewModel() {
    val toast = toastRepository.toast
    val toastRes = toastRepository.toastRes
}
