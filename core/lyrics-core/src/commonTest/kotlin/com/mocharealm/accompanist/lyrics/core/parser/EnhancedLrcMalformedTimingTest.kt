package com.mocharealm.accompanist.lyrics.core.parser

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import kotlin.test.Test
import kotlin.test.assertEquals

class EnhancedLrcMalformedTimingTest {
    @Test
    fun acceptsOutOfOrderWordTimestamps() {
        val parsed = EnhancedLrcParser.parse(
            "[01:54.40]<01:54.401>扮<01:54.601>弱<01:54.600>柳<01:55.000>争<01:55.310>取",
        )

        val line = parsed.lines.single() as KaraokeLine

        assertEquals("扮弱柳争取", line.syllables.joinToString("") { it.content })
    }
}
