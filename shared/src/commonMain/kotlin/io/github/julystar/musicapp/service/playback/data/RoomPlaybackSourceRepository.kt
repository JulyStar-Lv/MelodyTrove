package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.database.TrackSourcePlaybackCandidate
import io.github.julystar.musicapp.database.TrackSourceRefDao
import io.github.julystar.musicapp.platform.currentTimeMillis
import io.github.julystar.musicapp.service.playback.domain.PlaybackSourceOption
import io.github.julystar.musicapp.service.playback.domain.PlaybackSourceRepository

class RoomPlaybackSourceRepository(
    private val trackSourceRefDao: TrackSourceRefDao,
) : PlaybackSourceRepository {
    override suspend fun sources(trackId: Long): List<PlaybackSourceOption> {
        val candidates = trackSourceRefDao.playbackCandidates(trackId)
        val selectedSourceItemId = candidates
            .filter { candidate -> candidate.ref.isPreferred }
            .singleOrNull()
            ?.item
            ?.id
            ?: candidates.firstOrNull()?.item?.id
        return candidates.map { candidate ->
            candidate.toPlaybackSourceOption(
                isSelected = candidate.item.id == selectedSourceItemId,
            )
        }
    }

    override suspend fun select(trackId: Long, sourceItemId: Long): Boolean {
        return trackSourceRefDao.selectPreferredSource(
            trackId = trackId,
            sourceItemId = sourceItemId,
            now = currentTimeMillis(),
        )
    }
}

private fun TrackSourcePlaybackCandidate.toPlaybackSourceOption(
    isSelected: Boolean,
): PlaybackSourceOption {
    val quality = listOfNotNull(
        ref.codec?.uppercase(),
        ref.sampleRate?.takeIf { it > 0 }?.let { sampleRate -> "${sampleRate / 1_000.0} kHz" },
        ref.bitsPerSample?.takeIf { it > 0 }?.let { bits -> "$bits-bit" },
    ).joinToString(" · ").takeIf(String::isNotBlank)
    return PlaybackSourceOption(
        sourceItemId = item.id,
        accountName = account.displayName,
        displayName = item.displayName,
        quality = quality,
        isSelected = isSelected,
    )
}
