package com.github.tidetunes.plugin

import com.github.tidetunes.plugin.install.PluginInstaller
import com.github.tidetunes.plugin.install.FakePluginDao
import com.github.tidetunes.plugin.runtime.InstalledPlugin
import com.github.tidetunes.plugin.runtime.LyricoJsMetaSource
import com.github.tidetunes.plugin.runtime.PluginCandidateContextStore
import com.github.tidetunes.plugin.runtime.PluginConfigProvider
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
    fun importsAggregateZipAndRunsInstalledPlugin() = runTest {
        val temp = Files.createTempDirectory("tidetunes-plugin-import-test")
        val zip = temp.resolve("plugins.zip")
        writeZip(
            zip,
            mapOf(
                "metadata/manifest.json" to """
                    {
                      "id": "com.tidetunes.test.imported",
                      "name": "Imported Metadata",
                      "versionCode": 1,
                      "versionName": "1.0.0",
                      "author": "TideTunes",
                      "description": "Import/runtime smoke test",
                      "apiVersion": 3,
                      "minHostApiVersion": 3,
                      "entry": "source.js",
                      "includeDirs": ["lib"],
                      "capabilities": ["searchSongs", "getLyrics", "searchCovers"],
                      "configFields": [{"key":"region","title":"Region","defaultValue":"us"}]
                    }
                """.trimIndent(),
                "metadata/lib/01_helper.js" to """
                    function importedHelper(value) {
                      return "helper:" + value;
                    }
                """.trimIndent(),
                "metadata/source.js" to """
                    function searchSongs(request) {
                      var info = Platform.runtime.getInfo();
                      return {
                        songs: [{
                          id: "song-1",
                          title: importedHelper(request.title),
                          artist: request.artist || "",
                          fields: {
                            md5: Platform.crypto.md5("abc"),
                            hostApiVersion: String(info.hostApiVersion)
                          },
                          internal: { lyricToken: "opaque" }
                        }]
                      };
                    }
                    function getLyrics(request) {
                      return {
                        lines: [{
                          text: request.fields.md5 + ":" + request.internal.lyricToken,
                          startMs: 10,
                          endMs: 20,
                          words: [{ text: "opaque", startMs: 10, endMs: 20 }],
                          translation: "translated",
                          romanization: "romanized"
                        }],
                        rawPlainLrc: "[00:00.01]opaque"
                      };
                    }
                    function searchCovers(request) {
                      return { covers: [{ url: "https://example.test/cover.jpg", width: 300, height: 300 }] };
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
        val entity = dao.findByPluginId("com.tidetunes.test.imported")
        assertNotNull(entity)
        assertFalse(entity.enabled)

        val descriptor = PluginRuntimeDescriptor(
            pluginId = entity.pluginId,
            pluginName = entity.name,
            pluginVersionCode = entity.versionCode,
            pluginUpdatedAt = entity.updatedAt,
            entryFile = entity.entryFile,
            includeDirs = listOf("lib"),
            directory = (pluginsDir / entity.pluginId).toString(),
        )
        val manager = PluginRuntimeManager(
            PluginRuntimeFactory(
                PluginRuntimeSettings(
                    appVersionName = "test",
                    cacheDirectory = temp.resolve("cache").toString(),
                ),
            ),
            PluginScriptBundleBuilder(),
        )
        val source = LyricoJsMetaSource(
            plugin = InstalledPlugin(
                descriptor = descriptor,
                capabilities = setOf("searchSongs", "getLyrics", "searchCovers"),
                enabled = true,
            ),
            runtimeManager = manager,
            configProvider = PluginConfigProvider { mapOf("region" to "us") },
            resultParser = PluginResultParser(PluginCandidateContextStore()),
        )

        val songs = source.searchSongs(MetaSongQuery(title = "Title", artist = "Artist"))
        assertEquals(1, songs.size)
        assertEquals("helper:Title", songs[0].title)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", songs[0].fields["md5"])
        assertEquals("3", songs[0].fields["hostApiVersion"])
        assertNotNull(songs[0].contextToken)

        val lyrics = source.getLyrics(songs[0])
        assertNotNull(lyrics)
        assertEquals("900150983cd24fb0d6963f7d28e17f72:opaque", lyrics.lines.single().text)
        assertEquals("translated", lyrics.lines.single().translation)

        val covers = source.searchCovers(MetaSongQuery(title = "Title"))
        assertEquals("https://example.test/cover.jpg", covers.single().url)

        manager.closeAll()
    }

    private fun writeZip(path: java.nio.file.Path, entries: Map<String, String>) {
        ZipOutputStream(path.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }
}
