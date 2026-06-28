package com.github.tidetunes.feature.dashboard.presentation

import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardStateTest {

    @Test
    fun `default state has sleep timer disabled`() {
        val state = DashboardState()

        assertFalse(state.sleepEnabled)
        assertEquals(0, state.sleepHour)
        assertEquals(0, state.sleepMinute)
        assertEquals(persistentListOf(), state.importJobs)
    }

    @Test
    fun `sleep timer state carries enabled flag and time`() {
        val state = DashboardState(sleepEnabled = true, sleepHour = 1, sleepMinute = 30)

        assertTrue(state.sleepEnabled)
        assertEquals(1, state.sleepHour)
        assertEquals(30, state.sleepMinute)
    }
}

class ImportJobUiTest {

    @Test
    fun `active running job exposes running affordances`() {
        val job = ImportJobUi(
            id = "scan-1",
            status = LibrarySyncStatus.Running,
            scannedCount = 50,
            importedCount = 30,
            skippedCount = 15,
            failedCount = 5,
            checkpoint = null,
            errorMessage = null,
            hasError = false,
            isActive = true,
            canResume = false,
            canRetry = false,
            statusLabel = "Running",
        )

        assertTrue(job.isActive)
        assertFalse(job.canResume)
        assertFalse(job.canRetry)
        assertFalse(job.hasError)
    }

    @Test
    fun `paused job exposes resume affordance`() {
        val job = ImportJobUi(
            id = "scan-2",
            status = LibrarySyncStatus.Paused,
            scannedCount = 100,
            importedCount = 80,
            skippedCount = 10,
            failedCount = 10,
            checkpoint = null,
            errorMessage = null,
            hasError = false,
            isActive = false,
            canResume = true,
            canRetry = false,
            statusLabel = "Paused",
        )

        assertFalse(job.isActive)
        assertTrue(job.canResume)
        assertFalse(job.canRetry)
    }

    @Test
    fun `failed job exposes retry affordance and error`() {
        val job = ImportJobUi(
            id = "scan-3",
            status = LibrarySyncStatus.Failed,
            scannedCount = 0,
            importedCount = 0,
            skippedCount = 0,
            failedCount = 1,
            checkpoint = null,
            errorMessage = "Network timeout",
            hasError = true,
            isActive = false,
            canResume = false,
            canRetry = true,
            statusLabel = "Failed",
        )

        assertTrue(job.hasError)
        assertEquals("Network timeout", job.errorMessage)
        assertTrue(job.canRetry)
        assertFalse(job.isActive)
    }
}
