package io.github.julystar.musicapp.feature.settings.presentation

import io.github.julystar.musicapp.core.domain.model.SettingsCapabilities
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
