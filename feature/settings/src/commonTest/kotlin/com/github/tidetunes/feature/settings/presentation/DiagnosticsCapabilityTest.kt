package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.core.domain.model.SettingsCapabilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsCapabilityTest {
    @Test
    fun diagnosticsEntryRequiresCapabilityAndMatchingQuery() {
        assertFalse(shouldShowDiagnosticsCenter(SettingsCapabilities(), queryMatches = true))
        assertFalse(
            shouldShowDiagnosticsCenter(
                SettingsCapabilities(diagnosticsCenterSupported = true),
                queryMatches = false,
            )
        )
        assertTrue(
            shouldShowDiagnosticsCenter(
                SettingsCapabilities(diagnosticsCenterSupported = true),
                queryMatches = true,
            )
        )
    }
}
