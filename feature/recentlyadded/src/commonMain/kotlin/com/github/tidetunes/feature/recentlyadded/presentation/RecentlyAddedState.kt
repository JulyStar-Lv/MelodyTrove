package com.github.tidetunes.feature.recentlyadded.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.MediaId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class RecentlyAddedState(
    val isLoading: Boolean = true,
    val tracks: ImmutableList<RecentlyAddedTrackItem> = persistentListOf(),
    val error: String? = null,
)

@Immutable
data class RecentlyAddedTrackItem(
    val id: Long,
    val title: String,
    val artist: String?,
    val albumName: String?,
    val durationMs: Long?,
    val mediaId: MediaId?,
    val canDownload: Boolean,
)
