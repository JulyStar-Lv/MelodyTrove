package com.github.tidetunes.core.data.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileDiagnosticsServiceTest {
    @Test
    fun redactsCredentialsTokensAndAuthorizationValues() {
        val input = "https://user:secret@example.test/dav?access_token=token-value " +
            "refresh_token=refresh-value Authorization: Bearer bearer-value " +
            "api_key=api-value Cookie: sid=cookie-value"

        val redacted = input.redactSensitiveData()

        assertFalse(redacted.contains("user:secret"))
        assertFalse(redacted.contains("token-value"))
        assertFalse(redacted.contains("refresh-value"))
        assertFalse(redacted.contains("bearer-value"))
        assertFalse(redacted.contains("api-value"))
        assertFalse(redacted.contains("cookie-value"))
        assertTrue(redacted.contains("https://***:***@example.test"))
        assertTrue(redacted.contains("<REDACTED_QUERY>"))
        assertTrue(redacted.contains("refresh_token=***"))
    }
}
