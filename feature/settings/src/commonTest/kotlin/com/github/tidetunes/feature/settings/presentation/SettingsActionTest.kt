package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsActionTest {

    @Test
    fun `theme action carries selected mode`() {
        val action = SettingsAction.SetThemeMode(AppThemeMode.Dark)
        assertEquals(AppThemeMode.Dark, action.mode)
    }

    @Test
    fun `language action carries selected mode`() {
        val action = SettingsAction.SetLanguageMode(AppLanguageMode.English)
        assertEquals(AppLanguageMode.English, action.mode)
    }

    @Test
    fun `clear confirmation actions are singleton objects`() {
        assertEquals(SettingsAction.RequestClearAudio, SettingsAction.RequestClearAudio)
        assertEquals(SettingsAction.RequestClearImage, SettingsAction.RequestClearImage)
        assertEquals(SettingsAction.ConfirmPendingAction, SettingsAction.ConfirmPendingAction)
    }
}
