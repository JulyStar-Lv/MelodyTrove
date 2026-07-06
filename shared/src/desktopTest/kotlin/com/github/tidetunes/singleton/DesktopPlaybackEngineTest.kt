package com.github.tidetunes.singleton

import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineResource
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPlaybackEngineTest {
    @Test
    fun noopEngineReportsUnsupportedWithoutPlaybackState() {
        val engine = NoopDesktopPlaybackEngine()

        assertEquals(
            PlaybackEngineLoadResult.Unsupported(
                PlaybackEngineUnsupportedReason.MissingPlatformEngine
            ),
            engine.load(loadRequest()),
        )
        engine.play()
        engine.pause()
        engine.seekTo(1_000)
        engine.stop()

        assertEquals(0L, engine.readPosition().positionMs)
        assertEquals(0L, engine.readPosition().bufferedMs)
        assertEquals(0L, engine.readPosition().durationMs)
    }

    @Test
    fun rodioEngineDelegatesPlaybackCommandsToRuntime() {
        val runtime = RecordingDesktopRodioRuntime(loadResult = true)
        val engine = RodioDesktopPlaybackEngine(runtime)

        assertEquals(
            PlaybackEngineLoadResult.Ready,
            engine.load(loadRequest()),
        )
        engine.play()
        engine.pause()
        engine.seekTo(2_500)
        engine.stop()

        assertEquals(listOf("http://127.0.0.1/track.flac"), runtime.loadedUris)
        assertEquals(listOf(emptyMap()), runtime.loadedHeaders)
        assertEquals(1, runtime.playCalls)
        assertEquals(1, runtime.pauseCalls)
        assertEquals(listOf(2_500UL), runtime.seekCalls)
        assertEquals(1, runtime.stopCalls)
        assertEquals(1_000L, engine.readPosition().positionMs)
        assertEquals(1_000L, engine.readPosition().bufferedMs)
        assertEquals(123_000L, engine.readPosition().durationMs)
    }

    @Test
    fun rodioEnginePassesPlaybackHeadersToRuntime() {
        val runtime = RecordingDesktopRodioRuntime(loadResult = true)
        val engine = RodioDesktopPlaybackEngine(runtime)
        val request = loadRequest(
            headers = mapOf(
                "Authorization" to "Bearer token",
                "User-Agent" to "TideTunes",
            )
        )

        assertEquals(PlaybackEngineLoadResult.Ready, engine.load(request))

        assertEquals(listOf(request.resource.uri), runtime.loadedUris)
        assertEquals(listOf(request.resource.headers), runtime.loadedHeaders)
    }

    @Test
    fun rodioEngineReportsUnsupportedWhenRuntimeCannotLoad() {
        val runtime = RecordingDesktopRodioRuntime(loadResult = false)
        val engine = RodioDesktopPlaybackEngine(runtime)

        assertEquals(
            PlaybackEngineLoadResult.Unsupported(
                PlaybackEngineUnsupportedReason.UnsupportedResource
            ),
            engine.load(loadRequest()),
        )
        assertFalse(runtime.loaded)
        assertEquals(listOf("http://127.0.0.1/track.flac"), runtime.loadedUris)
    }

    @Test
    fun rodioEngineDoesNotLoadExpiredResource() {
        val runtime = RecordingDesktopRodioRuntime(loadResult = true)
        val engine = RodioDesktopPlaybackEngine(runtime)
        val request = loadRequest().copy(
            resource = loadRequest().resource.copy(expiresAtEpochMs = 1)
        )

        assertEquals(
            PlaybackEngineLoadResult.Failure(PlaybackEngineFailureReason.ExpiredResource),
            engine.load(request),
        )
        assertEquals(emptyList(), runtime.loadedUris)
    }

    private fun loadRequest(
        headers: Map<String, String> = emptyMap(),
    ): PlaybackEngineLoadRequest {
        return PlaybackEngineLoadRequest(
            item = PlayableItem(title = "Track", libraryTrackId = 1),
            resource = PlaybackEngineResource(
                uri = "http://127.0.0.1/track.flac",
                headers = headers,
            ),
        )
    }
}

private class RecordingDesktopRodioRuntime(
    private val loadResult: Boolean,
) : DesktopRodioRuntime {
    val loadedUris = mutableListOf<String>()
    val loadedHeaders = mutableListOf<Map<String, String>>()
    val seekCalls = mutableListOf<ULong>()
    var loaded = false
        private set
    var playCalls = 0
        private set
    var pauseCalls = 0
        private set
    var stopCalls = 0
        private set

    override fun load(uri: String, headers: Map<String, String>): Boolean {
        loadedUris += uri
        loadedHeaders += headers
        loaded = loadResult
        return loadResult
    }

    override fun play() {
        assertTrue(loaded)
        playCalls += 1
    }

    override fun pause() {
        assertTrue(loaded)
        pauseCalls += 1
    }

    override fun stop() {
        loaded = false
        stopCalls += 1
    }

    override fun seek(ms: ULong) {
        assertTrue(loaded)
        seekCalls += ms
    }

    override fun currentPositionMs(): Long = 1_000L

    override fun bufferedPositionMs(): Long = 1_000L

    override fun durationMs(): Long = 123_000L
}
