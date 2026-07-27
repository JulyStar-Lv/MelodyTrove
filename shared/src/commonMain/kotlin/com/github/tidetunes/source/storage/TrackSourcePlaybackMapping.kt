package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.TrackSourcePlaybackCandidate
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.legacyStorageTrackMediaId

internal fun TrackSourcePlaybackCandidate.toSourceTrackMediaIdOrNull(): MediaId? {
    val sourceId = account.providerType.toSourceId()
    return when (account.providerType) {
        ProviderTypes.Local,
        ProviderTypes.WebDav,
        ProviderTypes.OneDrive,
        ProviderTypes.Smb,
        -> item.canonicalPath?.takeIf { it.isNotBlank() }?.let { path ->
            legacyStorageTrackMediaId(
                sourceId = sourceId,
                accountId = storageSourceAccountId(account.id),
                path = path,
            )
        }
        else -> item.providerItemId.takeIf { it?.isNotBlank() == true }?.let { remoteId ->
            MediaId(
                sourceId = sourceId,
                mediaType = MediaType.Track,
                remoteId = remoteId,
            )
        }
    }
}

private fun String.toSourceId(): SourceId = when (this) {
    ProviderTypes.Local -> BuiltInSourceIds.Local
    ProviderTypes.WebDav -> BuiltInSourceIds.WebDav
    ProviderTypes.OneDrive -> BuiltInSourceIds.OneDrive
    ProviderTypes.Smb -> BuiltInSourceIds.Smb
    ProviderTypes.Navidrome -> BuiltInSourceIds.Navidrome
    ProviderTypes.OpenSubsonic -> BuiltInSourceIds.OpenSubsonic
    ProviderTypes.Emby -> BuiltInSourceIds.Emby
    else -> SourceId(this)
}
