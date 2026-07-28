package io.github.julystar.musicapp.core.lyrics.ui.reference

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferenceKaraokeRendererTest {
    @Test
    fun groupsLatinSyllablesByTrailingWordBoundary() {
        val syllables = listOf(
            KaraokeSyllable("Hel", 0, 300),
            KaraokeSyllable("lo ", 300, 600),
            KaraokeSyllable("world", 600, 1_100),
        )

        assertEquals(
            listOf(listOf("Hel", "lo "), listOf("world")),
            groupIntoWords(syllables).map { word -> word.map(KaraokeSyllable::content) },
        )
    }

    @Test
    fun usesSimpleLiftForComplexScriptsButAdvancedAnimationForLatin() {
        assertTrue("逐字歌词".shouldUseSimpleAnimation())
        assertTrue("مرحبا".shouldUseSimpleAnimation())
        assertFalse("karaoke".shouldUseSimpleAnimation())
    }

    @Test
    fun referenceAnimationCurvesReturnToRest() {
        assertEquals(0f, DipAndRise.transform(0f), absoluteTolerance = 0.0001f)
        assertEquals(1f, DipAndRise.transform(1f), absoluteTolerance = 0.0001f)
        assertEquals(0f, Swell.transform(0f), absoluteTolerance = 0.0001f)
        assertEquals(0f, Swell.transform(1f), absoluteTolerance = 0.0001f)
    }
}
