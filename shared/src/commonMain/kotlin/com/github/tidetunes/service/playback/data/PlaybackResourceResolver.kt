package com.github.tidetunes.service.playback.data

import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.TrackSourceRefDao
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.PlaybackResource
import com.github.tidetunes.source.api.SourcePlaybackFailureReason
import com.github.tidetunes.source.api.SourcePlaybackResult
import com.github.tidetunes.source.api.LegacyStoragePlaybackResolver
import com.github.tidetunes.source.storage.LegacyStorageLookup
import com.github.tidetunes.source.api.legacyStorageTrackMediaId
import com.github.tidetunes.source.storage.toLegacyStorageSourceAccountId
import uniffi.tidetunes_core.Music
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

class PlaybackResourceResolver(
    private val storageLookup: LegacyStorageLookup,
    private val trackSourceRefDao: TrackSourceRefDao,
    private val sourceRegistry: MusicSourceRegistry,
    private val legacyStoragePlaybackResolver: LegacyStoragePlaybackResolver,
) {
    suspend fun resolve(music: Music): SourcePlaybackResult {
        val candidates = trackSourceRefDao.playbackCandidates(music.meta.id.value)
        for (candidate in candidates) {
            val path = candidate.item.canonicalPath ?: continue
            val sourceId = candidate.account.providerType.toBuiltInSourceId()
            val source = sourceRegistry.sourceOrNull(sourceId) ?: continue
            val result = source.resolvePlayback(
                legacyStorageTrackMediaId(
                    sourceId = sourceId,
                    accountId = StorageId(candidate.item.sourceAccountId).toLegacyStorageSourceAccountId(),
                    path = path,
                )
            )
            if (result is SourcePlaybackResult.Success) return result
        }

        val storage = storageLookup.storageForPlayback(music.loc.storageId)
            ?: return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.UnsupportedAccount)
        val sourceId = storage.typ.toBuiltInSourceId()
        val source = sourceRegistry.sourceOrNull(sourceId)
            ?: return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)

        return source.resolvePlayback(
            legacyStorageTrackMediaId(
                sourceId = sourceId,
                accountId = music.loc.storageId.toLegacyStorageSourceAccountId(),
                path = music.loc.path,
            )
        )
    }

    suspend fun release(resource: PlaybackResource) {
        legacyStoragePlaybackResolver.release(resource.uri)
    }

    suspend fun release(uri: String) {
        legacyStoragePlaybackResolver.release(uri)
    }

    suspend fun releaseAll() {
        legacyStoragePlaybackResolver.releaseAll()
    }
}

private fun StorageType.toBuiltInSourceId() = when (this) {
    StorageType.LOCAL -> BuiltInSourceIds.Local
    StorageType.WEBDAV -> BuiltInSourceIds.WebDav
    StorageType.ONE_DRIVE -> BuiltInSourceIds.OneDrive
}

private fun String.toBuiltInSourceId() = when (this) {
    ProviderTypes.Local -> BuiltInSourceIds.Local
    ProviderTypes.WebDav -> BuiltInSourceIds.WebDav
    ProviderTypes.OneDrive -> BuiltInSourceIds.OneDrive
    else -> BuiltInSourceIds.WebDav
}
