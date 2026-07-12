package com.github.tidetunes.plugin

import com.github.tidetunes.plugin.runtime.*
import com.github.tidetunes.source.api.*
import kotlinx.serialization.json.*
import kotlin.test.*

class PluginPipelineTest {
    private val testManifestJson = """{"id":"com.test.plugin","name":"Test","versionCode":1,"versionName":"1.0","author":"test","description":"desc","apiVersion":3,"entry":"source.js","capabilities":["searchSongs","getLyrics"],"configFields":[]}"""

    @Test
    fun manifestJsonParseRoundtrip() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(testManifestJson).jsonObject
        assertEquals("com.test.plugin", root["id"]?.jsonPrimitive?.content)
        assertEquals(3, root["apiVersion"]?.jsonPrimitive?.int)
        assertEquals("source.js", root["entry"]?.jsonPrimitive?.content ?: root["entryFile"]?.jsonPrimitive?.content)
    }

    @Test
    fun runtimeCacheKeyEquality() {
        val a = PluginRuntimeCacheKey("a", 1, 100, "abc")
        val b = PluginRuntimeCacheKey("a", 1, 100, "abc")
        val c = PluginRuntimeCacheKey("a", 1, 100, "def")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun metaLyricsStructuredParsing() {
        val raw = """{"lines":[{"text":"Hello","startMs":100,"endMs":200,"words":[{"text":"He","startMs":100,"endMs":140},{"text":"llo","startMs":150,"endMs":200}],"translation":"\u4f60\u597d","romanization":"haro"}],"rawPlainLrc":"[00:00.10]Hello","translated":"\u4f60\u597d"}"""
        val parser = PluginResultParser()
        val lyrics = parser.lyrics(raw)
        assertNotNull(lyrics)
        assertEquals(1, lyrics!!.lines.size)
        assertEquals("Hello", lyrics.lines[0].text)
        assertEquals(100L, lyrics.lines[0].startMs)
        assertEquals(2, lyrics.lines[0].words.size)
        assertEquals("He", lyrics.lines[0].words[0].text)
        assertEquals("\u4f60\u597d", lyrics.lines[0].translation)
        assertEquals("haro", lyrics.lines[0].romanization)
        assertEquals("\u4f60\u597d", lyrics.translated)
    }

    @Test
    fun metaSongFieldsAndInternalReserved() {
        val raw = """{"songs":[{"id":"x","title":"T","artist":"A","fields":{"album":"ALB"},"internal":{"token":"secret"}}]}"""
        val parser = PluginResultParser()
        val songs = parser.songs(raw)
        assertEquals(1, songs.size)
        assertEquals("x", songs[0].id)
        assertEquals("T", songs[0].title)
        assertEquals("A", songs[0].artist)
        assertEquals("ALB", songs[0].fields["album"])
        assertNotNull(songs[0].contextToken)
    }

    @Test
    fun coverCandidates() {
        val raw = """{"covers":[{"url":"https://img.example/1.jpg","width":600,"height":600}]}"""
        val parser = PluginResultParser()
        val covers = parser.covers(raw)
        assertEquals(1, covers.size)
        assertEquals("https://img.example/1.jpg", covers[0].url)
        assertEquals(600, covers[0].width)
    }

    @Test
    fun metaSourceRegistryEmptyByDefault() {
        val registry = MetaSourceRegistry(emptyList())
        assertTrue(registry.sources.isEmpty())
    }

    @Test
    fun installedPluginDefaultsDisabled() {
        val plugin = InstalledPlugin(
            descriptor = PluginRuntimeDescriptor("test", "Test", 1, 0, "source.js", emptyList(), "/tmp"),
            capabilities = setOf("searchSongs")
        )
        assertFalse(plugin.enabled)
        assertFalse(plugin.allowAutomaticLookup)
    }
}
