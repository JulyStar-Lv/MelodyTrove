package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.model.NetworkStatus
import io.github.julystar.musicapp.core.domain.repository.NetworkStatusProvider
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngine
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineLoadRequest
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineLoadResult
import io.github.julystar.musicapp.source.api.PlaybackResource
import io.github.julystar.musicapp.source.api.SourcePlaybackResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import uniffi.app_backend.Music

internal sealed interface PlaybackPreparationResult {
    data class Ready(val resource: PlaybackResource) : PlaybackPreparationResult
    data object NetworkBlocked : PlaybackPreparationResult
    data object Failed : PlaybackPreparationResult
}

internal suspend fun preparePlayback(
    music: Music,
    playlistId: Long,
    playbackResourceResolver: PlaybackResourceResolver,
    playbackEngine: PlaybackEngine,
    settingsRepository: SettingsRepository?,
    networkStatusProvider: NetworkStatusProvider?,
): PlaybackPreparationResult {
    val persistedSettings = settingsRepository?.settings?.first()
    val settings = persistedSettings ?: AppSettings.Default
    val maxAttempts = if (persistedSettings?.retryPlaybackOnFailure == true) {
        settings.networkRetryCount + 1
    } else {
        1
    }
    repeat(maxAttempts) { attempt ->
        val result = withTimeoutOrNull(settings.connectionTimeoutSeconds * 1_000L) {
            when (val resolved = playbackResourceResolver.resolve(music)) {
                is SourcePlaybackResult.Failure -> PlaybackPreparationResult.Failed
                is SourcePlaybackResult.Success -> {
                    val resource = resolved.resource
                    val network = networkStatusProvider?.status?.value ?: NetworkStatus.Unknown
                    if (!resource.isLocal && (!network.isOnline ||
                            (network.isMetered && !settings.allowMeteredStreaming))) {
                        playbackResourceResolver.release(resource)
                        PlaybackPreparationResult.NetworkBlocked
                    } else {
                        when (
                            playbackEngine.load(
                                PlaybackEngineLoadRequest(
                                    item = music.toPlayableItem(playlistId),
                                    resource = resource.toPlaybackEngineResource(),
                                )
                            )
                        ) {
                            PlaybackEngineLoadResult.Ready -> PlaybackPreparationResult.Ready(resource)
                            is PlaybackEngineLoadResult.Unsupported,
                            is PlaybackEngineLoadResult.Failure -> {
                                playbackResourceResolver.release(resource)
                                PlaybackPreparationResult.Failed
                            }
                        }
                    }
                }
            }
        } ?: PlaybackPreparationResult.Failed
        if (result != PlaybackPreparationResult.Failed || attempt == maxAttempts - 1) {
            return result
        }
        delay(RETRY_DELAY_MS)
    }
    return PlaybackPreparationResult.Failed
}

private const val RETRY_DELAY_MS = 350L
