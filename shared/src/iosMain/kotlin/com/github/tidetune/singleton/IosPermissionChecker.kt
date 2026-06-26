package com.github.tidetune.singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IosPermissionChecker : PermissionChecker {
    override val havePermission: StateFlow<Boolean> = MutableStateFlow(true)

    override fun requestStoragePermission() {
        // TideTune reads only its sandbox and remote providers on iOS.
    }
}
