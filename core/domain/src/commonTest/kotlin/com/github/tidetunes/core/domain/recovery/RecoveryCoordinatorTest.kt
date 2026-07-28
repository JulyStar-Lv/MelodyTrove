package com.github.tidetunes.core.domain.recovery

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecoveryCoordinatorTest {
    @Test
    fun clearsMarkerOnlyAfterInitializationAndStableVerification() = runTest {
        val runtime = FakeRecoveryRuntime(stable = true)
        val result = RecoveryCoordinator(runtime).attempt(
            incidentIds = listOf("incident"),
            disabledComponents = setOf("third_party_plugins"),
        )

        assertIs<RecoveryResult.Success>(result)
        assertEquals(
            listOf("mark", "initialize", "stable", "resolve", "clear"),
            runtime.events,
        )
    }

    @Test
    fun failedInitializationKeepsMarkerAndIncidentUnresolved() = runTest {
        val runtime = FakeRecoveryRuntime(stable = false)
        val result = RecoveryCoordinator(runtime).attempt(
            incidentIds = listOf("incident"),
            disabledComponents = emptySet(),
        )

        assertIs<RecoveryResult.Failure>(result)
        assertEquals(listOf("mark", "initialize", "stable"), runtime.events)
    }
}

private class FakeRecoveryRuntime(
    private val stable: Boolean,
) : RecoveryRuntime {
    val events = mutableListOf<String>()

    override suspend fun markRecoveryAttempted(
        incidentIds: List<String>,
        disabledComponents: Set<String>,
    ) {
        events += "mark"
    }

    override suspend fun initializeNormalComponents(disabledComponents: Set<String>) {
        events += "initialize"
    }

    override suspend fun awaitStableStartup(): Boolean {
        events += "stable"
        return stable
    }

    override suspend fun resolveIncidents(incidentIds: List<String>) {
        events += "resolve"
    }

    override suspend fun clearPendingRecovery() {
        events += "clear"
    }
}
