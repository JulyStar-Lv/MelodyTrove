package io.github.julystar.musicapp.domain.importing

import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataNameSplitterTest {
    @Test
    fun splitsEveryConfiguredSeparator() {
        assertEquals(
            listOf("A", "B", "C", "D"),
            splitMetadataNames("A; B/C、D", ";/、", "", ignoreCase = false),
        )
    }

    @Test
    fun preservesSeparatorsInsideProtectedNames() {
        assertEquals(
            listOf("AC/DC", "Guest"),
            splitMetadataNames("AC/DC;Guest", ";/", "ac/dc", ignoreCase = true),
        )
    }
}
