package com.github.tidetunes.feature.settings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SettingsActionTest {

    @Test
    fun `NavigateToLog is a singleton object`() {
        assertEquals(SettingsAction.NavigateToLog, SettingsAction.NavigateToLog)
    }

    @Test
    fun `NavigateToDebugMore is a singleton object`() {
        assertEquals(SettingsAction.NavigateToDebugMore, SettingsAction.NavigateToDebugMore)
    }

    @Test
    fun `OpenGitRepo carries URL value`() {
        val action = SettingsAction.OpenGitRepo("https://github.com/example/repo")
        assertEquals("https://github.com/example/repo", action.url)
    }
}

class DebugActionTest {

    @Test
    fun `TriggerRustError is a singleton object`() {
        assertEquals(DebugAction.TriggerRustError, DebugAction.TriggerRustError)
    }

    @Test
    fun `TriggerRustAsyncError is a singleton object`() {
        assertEquals(DebugAction.TriggerRustAsyncError, DebugAction.TriggerRustAsyncError)
    }

    @Test
    fun `TriggerRustPanic is a singleton object`() {
        assertEquals(DebugAction.TriggerRustPanic, DebugAction.TriggerRustPanic)
    }

    @Test
    fun `TriggerKotlinError is a singleton object`() {
        assertEquals(DebugAction.TriggerKotlinError, DebugAction.TriggerKotlinError)
    }

    @Test
    fun `TriggerKotlinAsyncError is a singleton object`() {
        assertEquals(DebugAction.TriggerKotlinAsyncError, DebugAction.TriggerKotlinAsyncError)
    }
}
