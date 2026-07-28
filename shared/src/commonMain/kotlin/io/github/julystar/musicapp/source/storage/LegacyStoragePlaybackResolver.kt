package io.github.julystar.musicapp.source.storage

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.singleton.Bridge
import io.github.julystar.musicapp.source.api.LegacyStorageKind
import io.github.julystar.musicapp.source.api.LegacyStoragePlaybackResolver
import io.github.julystar.musicapp.source.api.PlaybackResource
import io.github.julystar.musicapp.source.api.SourcePlaybackFailureReason
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uniffi.app_backend.PlaybackSession
import uniffi.app_backend.Storage
import uniffi.app_backend.StorageEntryLoc
import uniffi.app_backend.StorageId
import uniffi.app_backend.StorageType
import uniffi.app_backend.ctCreatePlaybackSession

fun interface LegacyStorageLookup {
    suspend fun storageForPlayback(storageId: StorageId): Storage?
}

fun interface LegacyPlaybackSessionFactory {
    suspend fun create(storage: Storage, path: String): LegacyPlaybackSession?
}

interface LegacyPlaybackSession {
    val url: String

    fun shutdown()
}

class RetainedLegacyStoragePlaybackResolver(
    private val storageLookup: LegacyStorageLookup,
    private val sessionFactory: LegacyPlaybackSessionFactory,
) : LegacyStoragePlaybackResolver {
    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, LegacyPlaybackSession>()

    override suspend fun resolve(
        accountId: SourceAccountId,
        path: String,
        expectedStorageKind: LegacyStorageKind,
    ): SourcePlaybackResult {
        val expectedStorageType = expectedStorageKind.toStorageType()
        val storageId = accountId.toLegacyStorageIdOrNull()
            ?: return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedAccount)
        val storage = storageLookup.storageForPlayback(storageId)
            ?: return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedAccount)
        if (storage.typ != expectedStorageType) {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedAccount)
        }

        val session = sessionFactory.create(storage, path)
            ?: return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unknown)
        val uri = session.url
        if (uri.isBlank()) {
            session.shutdown()
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unknown)
        }

        mutex.withLock {
            sessions.put(uri, session)?.shutdown()
        }
        return SourcePlaybackResult.Success(
            PlaybackResource(
                uri = uri,
                mimeType = mimeTypeFromPath(path),
                isLocal = expectedStorageKind == LegacyStorageKind.Local,
            )
        )
    }

    override suspend fun release(uri: String) {
        val session = mutex.withLock {
            sessions.remove(uri)
        }
        session?.shutdown()
    }

    override suspend fun releaseAll() {
        val activeSessions = mutex.withLock {
            sessions.values.toList().also {
                sessions.clear()
            }
        }
        activeSessions.forEach { session ->
            session.shutdown()
        }
    }
}

class BridgeLegacyPlaybackSessionFactory(
    private val bridge: Bridge,
) : LegacyPlaybackSessionFactory {
    override suspend fun create(storage: Storage, path: String): LegacyPlaybackSession? {
        return bridge.run { backend ->
            ctCreatePlaybackSession(
                backend = backend,
                storage = storage,
                loc = StorageEntryLoc(
                    storageId = storage.id,
                    path = path,
                ),
            )
        }?.let(::UniffiLegacyPlaybackSession)
    }
}

private class UniffiLegacyPlaybackSession(
    private val session: PlaybackSession,
) : LegacyPlaybackSession {
    override val url: String
        get() = session.url()

    override fun shutdown() {
        session.shutdown()
    }
}

private fun mimeTypeFromPath(path: String): String? {
    val extension = path.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
    return when (extension) {
        "flac" -> "audio/flac"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wav" -> "audio/wav"
        else -> null
    }
}
