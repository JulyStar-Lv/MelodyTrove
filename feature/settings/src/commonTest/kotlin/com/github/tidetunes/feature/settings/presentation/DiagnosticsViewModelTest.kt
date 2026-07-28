package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.core.domain.model.DiagnosticIncidentSeverity
import com.github.tidetunes.core.domain.model.DiagnosticIncidentState
import com.github.tidetunes.core.domain.model.DiagnosticIncidentType
import com.github.tidetunes.core.domain.model.DiagnosticLogCategory
import com.github.tidetunes.core.domain.model.DiagnosticLogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiagnosticsViewModelTest {
    @Test
    fun buildsPagedLogFilterFromUiState() {
        val filter = DiagnosticsUiState(
            selectedSessionId = "session",
            logKeyword = "  backend  ",
            logLevel = DiagnosticLogLevel.Error,
            logCategory = DiagnosticLogCategory.Playback,
            logWindowMs = 60_000,
        ).logFilter(
            sessionId = "session",
            offset = 100,
            nowEpochMs = 1_000_000,
        )

        assertEquals(setOf("session"), filter.sessionIds)
        assertEquals(setOf(DiagnosticLogLevel.Error), filter.levels)
        assertEquals(setOf(DiagnosticLogCategory.Playback), filter.categories)
        assertEquals("backend", filter.keyword)
        assertEquals(940_000, filter.startEpochMs)
        assertEquals(100, filter.offset)
        assertEquals(100, filter.limit)
    }

    @Test
    fun buildsPagedIncidentFilterFromUiState() {
        val filter = DiagnosticsUiState(
            incidentType = DiagnosticIncidentType.AndroidAnr,
            incidentSeverity = DiagnosticIncidentSeverity.Warning,
            incidentState = DiagnosticIncidentState.PendingReview,
        ).incidentFilter(offset = 200)

        assertEquals(setOf(DiagnosticIncidentType.AndroidAnr), filter.types)
        assertEquals(setOf(DiagnosticIncidentSeverity.Warning), filter.severities)
        assertEquals(setOf(DiagnosticIncidentState.PendingReview), filter.states)
        assertEquals(200, filter.offset)
        assertEquals(100, filter.limit)
    }

    @Test
    fun exportResultEventsUpdateStatusAndKeepSuccessfulPath() {
        val success = DiagnosticsUiState().reduceExportEvent(
            DiagnosticsExportEvent.Completed(
                path = "/diagnostics/report.zip",
                presentationError = null,
            ),
        )
        assertEquals("/diagnostics/report.zip", success.lastExportPath)
        assertEquals("Diagnostics bundle is ready", success.status)

        val failure = DiagnosticsUiState().reduceExportEvent(
            DiagnosticsExportEvent.Failed("Export failed"),
        )
        assertNull(failure.lastExportPath)
        assertEquals("Export failed", failure.status)
    }
}
