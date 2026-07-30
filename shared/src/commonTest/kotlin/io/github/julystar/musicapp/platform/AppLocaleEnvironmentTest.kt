package io.github.julystar.musicapp.platform

import io.github.julystar.musicapp.core.domain.model.AppLanguageMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLocaleEnvironmentTest {
    @Test
    fun languageModesMapToResourceLocaleTags() {
        assertNull(AppLanguageMode.System.resourceLocaleTag())
        assertEquals("zh-Hans", AppLanguageMode.Chinese.resourceLocaleTag())
        assertEquals("en", AppLanguageMode.English.resourceLocaleTag())
    }
}
