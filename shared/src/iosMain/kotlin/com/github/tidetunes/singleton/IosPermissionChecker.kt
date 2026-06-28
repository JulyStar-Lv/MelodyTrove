package com.github.tidetunes.singleton

import com.github.tidetunes.core.domain.repository.PermissionChecker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IosPermissionChecker : PermissionChecker {
    override val havePermission: StateFlow<Boolean> = MutableStateFlow(true)

    override fun requestStoragePermission() {
        // TideTunes reads only its sandbox and remote providers on iOS.
    }
}
