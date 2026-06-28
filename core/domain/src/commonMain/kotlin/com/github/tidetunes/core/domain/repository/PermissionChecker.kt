package com.github.tidetunes.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PermissionChecker {
    val havePermission: StateFlow<Boolean>
    fun requestStoragePermission()
}
