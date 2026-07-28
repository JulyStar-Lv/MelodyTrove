package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.database.ProviderTypes
import io.github.julystar.musicapp.database.TrackSourceRefDao
import io.github.julystar.musicapp.source.api.BuiltInSourceIds
import io.github.julystar.musicapp.source.api.MusicSourceRegistry
import io.github.julystar.musicapp.source.api.PlaybackResource
import io.github.julystar.musicapp.source.api.SourcePlaybackFailureReason
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import io.github.julystar.musicapp.source.api.LegacyStoragePlaybackResolver
import io.github.julystar.musicapp.source.storage.LegacyStorageLookup
import io.github.julystar.musicapp.source.api.legacyStorageTrackMediaId
import io.github.julystar.musicapp.source.storage.toLegacyStorageSourceAccountId
import uniffi.app_backend.Music
import uniffi.app_backend.StorageId
import uniffi.app_backend.StorageType

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
    StorageType.SMB -> BuiltInSourceIds.Smb
}

private fun String.toBuiltInSourceId() = when (this) {
    ProviderTypes.Local -> BuiltInSourceIds.Local
    ProviderTypes.WebDav -> BuiltInSourceIds.WebDav
    ProviderTypes.OneDrive -> BuiltInSourceIds.OneDrive
    ProviderTypes.Smb -> BuiltInSourceIds.Smb
    else -> BuiltInSourceIds.WebDav
}
