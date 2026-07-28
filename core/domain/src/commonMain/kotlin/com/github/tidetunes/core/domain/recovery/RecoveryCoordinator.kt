package com.github.tidetunes.core.domain.recovery

interface RecoveryRuntime {
    suspend fun markRecoveryAttempted(incidentIds: List<String>, disabledComponents: Set<String>)
    suspend fun initializeNormalComponents(disabledComponents: Set<String>)
    suspend fun awaitStableStartup(): Boolean
    suspend fun resolveIncidents(incidentIds: List<String>)
    suspend fun clearPendingRecovery()
}

sealed interface RecoveryResult {
    data object Success : RecoveryResult
    data class Failure(val message: String) : RecoveryResult
}

class RecoveryCoordinator(
    private val runtime: RecoveryRuntime,
) {
    suspend fun attempt(
        incidentIds: List<String>,
        disabledComponents: Set<String>,
    ): RecoveryResult {
        return runCatching {
            runtime.markRecoveryAttempted(incidentIds, disabledComponents)
            runtime.initializeNormalComponents(disabledComponents)
            check(runtime.awaitStableStartup()) { "Startup did not reach the stable state" }
            runtime.resolveIncidents(incidentIds)
            runtime.clearPendingRecovery()
        }.fold(
            onSuccess = { RecoveryResult.Success },
            onFailure = { error ->
                RecoveryResult.Failure(error.message ?: "Recovery initialization failed")
            },
        )
    }
}
