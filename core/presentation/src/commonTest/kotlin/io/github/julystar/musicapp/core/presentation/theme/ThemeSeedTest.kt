package io.github.julystar.musicapp.core.presentation.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeSeedTest {

    @Test
    fun `manual theme color selection is enabled only when artwork color is off`() {
        assertFalse(canSelectManualThemeColor(artworkThemeEnabled = true))
        assertTrue(canSelectManualThemeColor(artworkThemeEnabled = false))
    }

    private val manual = 0xFFFF5B8AL
    private val artwork = 0xFF3D9AFFL
    private val previous = 0xFF7A6CFFL

    @Test
    fun `available artwork wins when artwork colors are enabled`() {
        val result = resolveThemeSeed(
            artworkThemeEnabled = true,
            artworkStatus = ArtworkThemeSeedStatus.Available,
            artworkSeedArgb = artwork,
            previousValidArtworkSeedArgb = previous,
            manualSeedArgb = manual,
        )

        assertEquals(artwork, result.effectiveSeedArgb)
        assertEquals(ThemeSeedSource.Artwork, result.source)
    }

    @Test
    fun `loading keeps previous artwork and otherwise falls back to manual`() {
        val previousResult = resolveThemeSeed(
            artworkThemeEnabled = true,
            artworkStatus = ArtworkThemeSeedStatus.Loading,
            artworkSeedArgb = null,
            previousValidArtworkSeedArgb = previous,
            manualSeedArgb = manual,
        )
        val fallbackResult = resolveThemeSeed(
            artworkThemeEnabled = true,
            artworkStatus = ArtworkThemeSeedStatus.Loading,
            artworkSeedArgb = null,
            previousValidArtworkSeedArgb = null,
            manualSeedArgb = manual,
        )

        assertEquals(previous, previousResult.effectiveSeedArgb)
        assertEquals(ThemeSeedSource.PreviousArtwork, previousResult.source)
        assertEquals(manual, fallbackResult.effectiveSeedArgb)
        assertEquals(ThemeSeedSource.Manual, fallbackResult.source)
    }

    @Test
    fun `missing failed and disabled artwork use manual seed`() {
        val statuses = listOf(
            ArtworkThemeSeedStatus.Missing,
            ArtworkThemeSeedStatus.Failed,
            ArtworkThemeSeedStatus.Disabled,
        )

        statuses.forEach { status ->
            val result = resolveThemeSeed(
                artworkThemeEnabled = true,
                artworkStatus = status,
                artworkSeedArgb = artwork,
                previousValidArtworkSeedArgb = previous,
                manualSeedArgb = manual,
            )
            assertEquals(manual, result.effectiveSeedArgb)
            assertEquals(ThemeSeedSource.Manual, result.source)
        }
    }

    @Test
    fun `turning artwork colors off ignores every artwork state`() {
        ArtworkThemeSeedStatus.entries.forEach { status ->
            val result = resolveThemeSeed(
                artworkThemeEnabled = false,
                artworkStatus = status,
                artworkSeedArgb = artwork,
                previousValidArtworkSeedArgb = previous,
                manualSeedArgb = manual,
            )
            assertEquals(manual, result.effectiveSeedArgb)
            assertEquals(ThemeSeedSource.Manual, result.source)
        }
    }
}
