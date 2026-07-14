package com.github.tidetunes.plugin

import com.github.tidetunes.plugin.install.FakePluginDao
import com.github.tidetunes.plugin.install.PluginInstaller
import com.github.tidetunes.plugin.runtime.InstalledPlugin
import com.github.tidetunes.plugin.runtime.LyricoJsMetaSource
import com.github.tidetunes.plugin.runtime.PluginCandidateContextStore
import com.github.tidetunes.plugin.runtime.PluginConfigProvider
import com.github.tidetunes.plugin.runtime.PluginLookupMode
import com.github.tidetunes.plugin.runtime.PluginResultParser
import com.github.tidetunes.plugin.runtime.PluginRuntimeDescriptor
import com.github.tidetunes.plugin.runtime.PluginRuntimeFactory
import com.github.tidetunes.plugin.runtime.PluginRuntimeManager
import com.github.tidetunes.plugin.runtime.PluginRuntimeSettings
import com.github.tidetunes.plugin.runtime.PluginScriptBundleBuilder
import com.github.tidetunes.source.api.MetaSongQuery
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

class PluginImportRuntimeDesktopTest {
    @Test
    fun importsStrictLyricoV3ZipAndRunsCompleteMetadataFlow() = runTest {
        val temp = Files.createTempDirectory("tidetunes-plugin-import-test")
        val zip = temp.resolve("plugin.zip")
        writeZip(
            zip,
            mapOf(
                "metadata/manifest.json" to
                    """
                    {
                      "id": "com.tidetunes.test.imported",
                      "name": "Imported Metadata",
                      "versionCode": 1,
                      "versionName": "1.0.0",
                      "author": "TideTunes Test",
                      "description": "Lyrico v3 import and runtime contract test",
                      "apiVersion": 3,
                      "minHostApiVersion": 3,
                      "entry": "source.js",
                      "includeDirs": ["lib"],
                      "capabilities": ["searchSongs", "getLyrics", "searchCovers"],
                      "configFields": [
                        {"key":"region","title":"Region","type":"text","defaultValue":"us"}
                      ]
                    }
                    """.trimIndent(),
                "metadata/lib/01_helper.js" to
                    """
                    function importedHelper(value) {
                      return "helper:" + value;
                    }
                    """.trimIndent(),
                "metadata/source.js" to
                    """
                    function searchSongs(request) {
                      if (!request.keyword || request.page !== 1 || request.pageSize !== 20) {
                        throw new Error("invalid Lyrico search request");
                      }
                      if (request.separator !== "/" || request.config.region !== "us") {
                        throw new Error("missing separator or merged config");
                      }
                      var info = Platform.runtime.getInfo();
                      return JSON.stringify([{
                        id: "song-1",
                        title: importedHelper(request.keyword),
                        artist: ["Artist A", "Artist B"],
                        album: "Album",
                        duration_ms: 123000,
                        cover_url: "https://example.test/cover.jpg",
                        fields: {
                          album: "Album",
                          md5: Platform.crypto.md5("abc"),
                          hostApiVersion: String(info.hostApiVersion)
                        },
                        internal: { lyric_id: "lyric-1" }
                      }]);
                    }

                    function getLyrics(request) {
                      if (!request.song || request.song.pluginId !== "com.tidetunes.test.imported") {
                        throw new Error("missing nested song request");
                      }
                      if (request.song.internal.lyric_id !== "lyric-1") {
                        throw new Error("private context was not returned");
                      }
                      return JSON.stringify({
                        type: "structured",
                        original: [
                          [0, 2000, request.song.fields.md5 + ":" + request.song.internal.lyric_id],
                          [2000, 4000, [[2000, 3000, "Second"], [3000, 4000, " line"]]]
                        ],
                        translated: [[0, 2000, "translated"]],
                        romanization: [[0, 2000, "romanized"]]
                      });
                    }

                    function searchCovers(request) {
                      if (!request.keyword || request.pageSize !== 5) {
                        throw new Error("invalid Lyrico cover request");
                      }
                      return JSON.stringify([{
                        id: "song-1",
                        picUrl: "https://example.test/cover.jpg"
                      }]);
                    }
                    """.trimIndent(),
            ),
        )

        val dao = FakePluginDao()
        val pluginsDir = temp.resolve("plugins").toString().toPath()
        val installer = PluginInstaller(dao, pluginsDir)
        val installResult = installer.installAllFromZip(zip.toString().toPath())

        assertTrue(installResult.failed.isEmpty(), installResult.failed.toString())
        assertEquals(1, installResult.installed.size)
        var entity = dao.findByPluginId("com.tidetunes.test.imported")
        assertNotNull(entity)
        assertFalse(entity.enabled)
        assertTrue(entity.allowManualLookup)
        assertFalse(entity.allowAutomaticLookup)
        assertFalse(entity.allowBatchLookup)
        assertEquals("us", dao.configValue(entity.pluginId, "region"))

        dao.setEnabled(entity.pluginId, true)
        entity = dao.findByPluginId(entity.pluginId)
        assertNotNull(entity)

        val descriptor = PluginRuntimeDescriptor(
            pluginId = entity.pluginId,
            pluginName = entity.name,
            pluginVersionCode = entity.versionCode,
            pluginUpdatedAt = entity.updatedAt,
            entryFile = entity.entryFile,
            includeDirs = listOf("lib"),
            directory = (pluginsDir / entity.pluginId).toString(),
        )
        val settings = PluginRuntimeSettings(
            appVersionName = "test",
            cacheDirectory = temp.resolve("cache").toString(),
        )
        val manager = PluginRuntimeManager(
            factory = PluginRuntimeFactory(settings),
            bundleBuilder = PluginScriptBundleBuilder(),
        )
        val parser = PluginResultParser(PluginCandidateContextStore())
        val source = LyricoJsMetaSource(
            plugin = InstalledPlugin(
                descriptor = descriptor,
                capabilities = setOf("searchSongs", "getLyrics", "searchCovers"),
                enabled = true,
            ),
            runtimeManager = manager,
            configProvider = PluginConfigProvider { pluginId ->
                dao.configsFor(pluginId).associate { it.configKey to it.configValue }
            },
            resultParser = parser,
        )

        val songs = source.searchSongs(
            MetaSongQuery(title = "Title", artist = "Artist"),
            PluginLookupMode.MANUAL,
        )
        assertEquals(1, songs.size)
        assertEquals("helper:Title Artist", songs[0].title)
        assertEquals("Artist A/Artist B", songs[0].artist)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", songs[0].fields["md5"])
        assertEquals("3", songs[0].fields["hostApiVersion"])
        assertNotNull(songs[0].contextToken)

        val lyrics = source.getLyrics(
            candidate = songs[0],
            config = emptyMap(),
            mode = PluginLookupMode.MANUAL,
        )
        assertNotNull(lyrics)
        assertEquals(
            "900150983cd24fb0d6963f7d28e17f72:lyric-1",
            lyrics.lines[0].text,
        )
        assertEquals("translated", lyrics.lines[0].translation)
        assertEquals("romanized", lyrics.lines[0].romanization)
        assertEquals("Second line", lyrics.lines[1].text)
        assertEquals(2, lyrics.lines[1].words.size)

        val covers = source.searchCovers(
            MetaSongQuery(title = "Title"),
            PluginLookupMode.MANUAL,
        )
        assertEquals("https://example.test/cover.jpg", covers.single().url)
        assertEquals("song-1", covers.single().sourceId)

        source.clearPrivateContexts()
        manager.closeAll()
        assertTrue(manager.cachedPluginIds().isEmpty())
    }

    private fun writeZip(
        path: java.nio.file.Path,
        entries: Map<String, String>,
    ) {
        ZipOutputStream(path.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }
}
