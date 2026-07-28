package io.github.julystar.musicapp.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PermissionChecker {
    val havePermission: StateFlow<Boolean>
    fun requestStoragePermission()
}
