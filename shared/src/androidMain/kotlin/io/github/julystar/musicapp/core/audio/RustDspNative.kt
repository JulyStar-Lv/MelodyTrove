package io.github.julystar.musicapp.core.audio

import java.nio.ByteBuffer

/**
 * Direct-buffer JNI entry points. The owning [uniffi.app_backend.NativeAudioDsp]
 * must outlive every call made with its handle.
 */
internal object RustDspNative {
    init {
        // UniFFI loads this through JNA, which does not register it for JVM JNI lookup.
        System.loadLibrary("app_backend")
    }

    external fun nativeConfigureFormat(
        handle: Long,
        sampleRate: Int,
        channels: Int,
    ): Int

    external fun nativeReset(handle: Long)

    external fun nativeProcessFloat(
        handle: Long,
        buffer: ByteBuffer,
        frames: Int,
        channels: Int,
    ): Int

    external fun nativeProcessI16(
        handle: Long,
        buffer: ByteBuffer,
        sampleCount: Int,
    ): Int
}
