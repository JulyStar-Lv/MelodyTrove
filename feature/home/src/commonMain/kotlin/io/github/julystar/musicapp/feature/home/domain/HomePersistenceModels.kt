package io.github.julystar.musicapp.feature.home.domain

import androidx.compose.runtime.Immutable
import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.presentation.components.QualityBadgeType
import kotlinx.serialization.Serializable

@Serializable
enum class PinnedItemType {
    Track,
    Playlist,
    Album,
    Artist,
}

@Serializable
@Immutable
data class PinnedHomeItem(
    val id: String,
    val type: PinnedItemType,
    val referenceId: Long,
    val order: Int,
    val pinnedAtEpochMs: Long,
)

@Immutable
data class HistoryPlayItem(
    val trackId: Long,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
    val mediaId: MediaId?,
    val playedAtEpochMs: Long,
    val artworkIndex: Int,
    val qualityBadge: QualityBadgeType? = null,
)
