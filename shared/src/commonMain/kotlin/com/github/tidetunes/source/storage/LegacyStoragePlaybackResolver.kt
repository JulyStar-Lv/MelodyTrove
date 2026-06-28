package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uniffi.tidetunes_core.PlaybackSession
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType
import uniffi.tidetunes_core.ctCreatePlaybackSession

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

interface LegacyStoragePlaybackResolver {
    suspend fun resolve(
        accountId: SourceAccountId,
        path: String,
        expectedStorageType: StorageType,
    ): SourcePlaybackResult

    suspend fun release(uri: String)

    suspend fun releaseAll()
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
        expectedStorageType: StorageType,
    ): SourcePlaybackResult {
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
                isLocal = expectedStorageType == StorageType.LOCAL,
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
