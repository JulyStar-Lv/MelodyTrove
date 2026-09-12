package io.github.julystar.musicapp.car.presentation.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomotiveArchitectureTest {
    private val root: Path = findRepositoryRoot()

    @Test
    fun carModulesRemainIndependentFromMobilePresentation() {
        val carAppBuild = root.resolve("carApp/build.gradle.kts").readText()
        val carPresentationBuild = root.resolve("car/presentation/build.gradle.kts").readText()
        val carSources = readKotlin(root.resolve("car/presentation/src/main")) +
            readKotlin(root.resolve("carApp/src/main"))

        assertFalse(carAppBuild.contains("project(\":androidApp\")"))
        assertFalse(carAppBuild.contains("project(\":shared\")"))
        assertFalse(carPresentationBuild.contains("project(\":shared\")"))
        assertFalse(carPresentationBuild.contains("service:playback:presentation"))
        assertFalse(carSources.contains("io.github.julystar.musicapp.feature."))
        assertFalse(carSources.contains("io.github.julystar.musicapp.presentation."))
    }

    @Test
    fun coreDoesNotDependOnCarPresentation() {
        val coreSources = readKotlin(root.resolve("core"))
        assertFalse(coreSources.contains("io.github.julystar.musicapp.car.presentation"))
    }

    @Test
    fun playerAndMediaSessionHaveOneRuntimeOwner() {
        val allSources = listOf("androidApp", "car", "carApp", "core", "feature", "service", "shared", "source")
            .joinToString("\n") { directory -> readKotlin(root.resolve(directory)) }
        assertEquals(1, Regex("ExoPlayer\\.Builder\\(").findAll(allSources).count())
        assertEquals(1, Regex("MediaLibrarySession\\.Builder\\(").findAll(allSources).count())
        val playbackService = root.resolve(
            "core/runtime/src/androidMain/kotlin/io/github/julystar/musicapp/core/PlaybackService.kt",
        )
        assertTrue(playbackService.exists())
    }

    @Test
    fun carLayoutDoesNotEncodeReferencePixelsAsDpOrExactWidthChecks() {
        val carSources = readKotlin(root.resolve("car/presentation/src/main")) +
            readKotlin(root.resolve("carApp/src/main"))
        assertFalse(Regex("(2496|1728|5120|1304|1080)\\.dp").containsMatchIn(carSources))
        assertFalse(Regex("screenWidth\\s*==\\s*(2496|1728|5120)").containsMatchIn(carSources))
        assertFalse(Regex("Modifier\\s*\\.\\s*scale\\(").containsMatchIn(carSources))
    }

    @Test
    fun carPresentationIsIsolatedFromAndroidPlatformAdapters() {
        val presentationSources = readKotlin(root.resolve("car/presentation/src/main"))
        listOf(
            "android.provider.Settings",
            "android.content.ContentResolver",
            "android.database.ContentObserver",
            "android.os.Environment",
        ).forEach { forbidden -> assertFalse(presentationSources.contains(forbidden), forbidden) }

        val presentationModule = root.resolve(
            "car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/di/CarPresentationModule.kt",
        ).readText()
        assertFalse(presentationModule.contains("DefaultCarLocalLibraryImporter"))
    }

    @Test
    fun hostPixelGeometryLivesOnlyInCarAppWindowAdapters() {
        val referencePixels = Regex("(?<![A-Za-z0-9_])(5120|2560|1440|2496|1728|832|192)(?![A-Za-z0-9_])")
        kotlinFiles(root.resolve("car/presentation/src/main"))
            .filterNot { it.fileName.toString() == "CarLayoutProfileResolver.kt" }
            .forEach { file ->
                assertFalse(referencePixels.containsMatchIn(file.readText()), file.toString())
            }

        val transformedReferenceGeometry = Regex("(?<![A-Za-z0-9_])(5600|1784|2518|1260)(?![A-Za-z0-9_])")
        kotlinFiles(root.resolve("car/presentation/src/main"))
            .filterNot { it.fileName.toString() == "CarLayoutProfileResolver.kt" }
            .forEach { file ->
                assertFalse(transformedReferenceGeometry.containsMatchIn(file.readText()), file.toString())
            }

        kotlinFiles(root.resolve("carApp/src/main"))
            .filter { file -> referencePixels.containsMatchIn(file.readText()) }
            .forEach { file -> assertTrue(file.toString().contains("/window/"), file.toString()) }
    }

    @Test
    fun runtimeDependencyDirectionRemainsHostOnly() {
        val carPresentationBuild = root.resolve("car/presentation/build.gradle.kts").readText()
        val corePresentationBuild = root.resolve("core/presentation/build.gradle.kts").readText()
        val coreDomainBuild = root.resolve("core/domain/build.gradle.kts").readText()

        assertFalse(carPresentationBuild.contains("project(\":core:runtime\")"))
        assertFalse(corePresentationBuild.contains("project(\":core:runtime\")"))
        listOf(":core:runtime", ":car:presentation", ":carApp", ":shared").forEach { forbidden ->
            assertFalse(coreDomainBuild.contains("project(\"$forbidden\")"), forbidden)
        }
    }

    @Test
    fun routeComposablesDelegateBusinessStateAndSideEffects() {
        val sourceRoot = root.resolve(
            "car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation",
        )
        val search = sourceRoot.resolve("screen/CarSearchScreen.kt").readText()
        val settings = sourceRoot.resolve("screen/CarSettingsScreen.kt").readText()
        val nowPlaying = sourceRoot.resolve("nowplaying/CarNowPlayingScreen.kt").readText()
        val fullscreen = sourceRoot.resolve("nowplaying/CarFullscreenNowPlayingScreen.kt").readText()
        val navigation = sourceRoot.resolve("navigation/CarNavigationRoot.kt").readText()

        assertFalse(search.contains("SearchRepository"))
        assertFalse(search.contains("FavoritesRepository"))
        assertFalse(search.contains("PlaybackController"))
        assertFalse(search.contains("rememberCoroutineScope"))
        assertFalse(settings.contains("SettingsRepository"))
        assertFalse(settings.contains("CarLocalLibraryImporter"))
        assertFalse(settings.contains("rememberCoroutineScope"))
        assertFalse(nowPlaying.contains("NowPlayingRepository"))
        assertFalse(nowPlaying.contains("PlaybackController"))
        assertFalse(nowPlaying.contains("FavoritesRepository"))
        assertFalse(fullscreen.contains("NowPlayingRepository"))
        assertFalse(fullscreen.contains("PlaybackController"))
        assertFalse(navigation.contains("LibraryRepository"))
        assertFalse(navigation.contains("PlaylistRepository"))
        assertFalse(navigation.contains("SearchRepository"))
        assertFalse(navigation.contains("SettingsRepository"))
        assertFalse(navigation.contains("PlaybackController"))
    }

    private fun readKotlin(path: Path): String {
        if (!path.exists()) return ""
        return kotlinFiles(path).joinToString("\n") { it.readText() }
    }

    private fun kotlinFiles(path: Path): List<Path> {
        if (!path.exists()) return emptyList()
        return Files.walk(path).use { paths ->
            paths.filter { file ->
                Files.isRegularFile(file) &&
                    file.fileName.toString().endsWith(".kt") &&
                    file.none { segment ->
                        val name = segment.toString()
                        name == "build" || name == "test" || name.endsWith("Test")
                    }
            }.toList()
        }
    }

    private fun findRepositoryRoot(): Path {
        var current: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (current != null) {
            if (current.resolve("settings.gradle.kts").exists() && current.resolve("carApp").exists()) return current
            current = current.parent
        }
        error("Could not locate TidePlayer repository root")
    }
}
