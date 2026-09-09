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
        assertFalse(Regex("(2496|1728|1080)\\.dp").containsMatchIn(carSources))
        assertFalse(Regex("screenWidth\\s*==\\s*(2496|1728)").containsMatchIn(carSources))
    }

    private fun readKotlin(path: Path): String {
        if (!path.exists()) return ""
        return Files.walk(path).use { paths ->
            paths.filter { file ->
                Files.isRegularFile(file) &&
                    file.fileName.toString().endsWith(".kt") &&
                    file.none { segment ->
                        val name = segment.toString()
                        name == "build" || name == "test" || name.endsWith("Test")
                    }
            }
                .map(Path::readText)
                .toList()
                .joinToString("\n")
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
