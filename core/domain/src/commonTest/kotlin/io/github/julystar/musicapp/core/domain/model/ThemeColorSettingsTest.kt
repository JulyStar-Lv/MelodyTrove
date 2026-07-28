package io.github.julystar.musicapp.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeColorSettingsTest {

    @Test
    fun `theme seed normalization keeps rgb and forces an opaque alpha`() {
        assertEquals(0xFFFF5B8AL, normalizeThemeSeedArgb(0x12FF5B8AL))
    }

    @Test
    fun `custom palette normalization removes duplicates and enforces the limit`() {
        val values = buildList {
            add(0x123D9AFFL)
            add(0xFF3D9AFFL)
            repeat(MAX_CUSTOM_THEME_SEEDS + 4) { index ->
                add(0xFF000000L or index.toLong())
            }
        }
        val normalized = normalizeCustomThemeSeedArgbValues(values)

        assertEquals(MAX_CUSTOM_THEME_SEEDS, normalized.size)
        assertEquals(0xFF3D9AFFL, normalized.first())
        assertEquals(normalized.distinct(), normalized)
    }
}
