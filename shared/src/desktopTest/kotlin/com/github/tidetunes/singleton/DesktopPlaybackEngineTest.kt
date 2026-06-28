package com.github.tidetunes.singleton

import com.github.tidetunes.service.playback.domain.PlayableItem
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
    fun mpvEngineDelegatesPlaybackCommandsToRuntime() {
        val runtime = RecordingDesktopMpvRuntime(loadResult = true)
        val engine = MpvDesktopPlaybackEngine(runtime)

        assertEquals(
            PlaybackEngineLoadResult.Ready,
            engine.load(loadRequest()),
        )
        engine.play()
        engine.pause()
        engine.seekTo(2_500)
        engine.stop()

        assertEquals(listOf("http://127.0.0.1/track.flac"), runtime.loadedUris)
        assertEquals(1, runtime.playCalls)
        assertEquals(1, runtime.pauseCalls)
        assertEquals(listOf(2_500UL), runtime.seekCalls)
        assertEquals(1, runtime.stopCalls)
        assertEquals(1_000L, engine.readPosition().positionMs)
        assertEquals(2_000L, engine.readPosition().bufferedMs)
        assertEquals(123_000L, engine.readPosition().durationMs)
    }

    @Test
    fun mpvEngineReportsUnsupportedWhenRuntimeCannotLoad() {
        val runtime = RecordingDesktopMpvRuntime(loadResult = false)
        val engine = MpvDesktopPlaybackEngine(runtime)

        assertEquals(
            PlaybackEngineLoadResult.Unsupported(
                PlaybackEngineUnsupportedReason.UnsupportedResource
            ),
            engine.load(loadRequest()),
        )
        assertFalse(runtime.loaded)
        assertEquals(listOf("http://127.0.0.1/track.flac"), runtime.loadedUris)
    }

    private fun loadRequest(): PlaybackEngineLoadRequest {
        return PlaybackEngineLoadRequest(
            item = PlayableItem(title = "Track", libraryTrackId = 1),
            resource = PlaybackEngineResource(uri = "http://127.0.0.1/track.flac"),
        )
    }
}

private class RecordingDesktopMpvRuntime(
    private val loadResult: Boolean,
) : DesktopMpvRuntime {
    val loadedUris = mutableListOf<String>()
    val seekCalls = mutableListOf<ULong>()
    var loaded = false
        private set
    var playCalls = 0
        private set
    var pauseCalls = 0
        private set
    var stopCalls = 0
        private set

    override fun load(uri: String): Boolean {
        loadedUris += uri
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

    override fun bufferedPositionMs(): Long = 2_000L

    override fun durationMs(): Long = 123_000L
}
