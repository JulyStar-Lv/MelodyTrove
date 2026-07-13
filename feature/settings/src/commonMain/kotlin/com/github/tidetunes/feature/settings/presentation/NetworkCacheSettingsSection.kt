package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AUDIO_CACHE_LIMIT_PRESETS_BYTES
import com.github.tidetunes.core.domain.model.IMAGE_CACHE_LIMIT_PRESETS_BYTES
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*

@Composable
fun NetworkCacheSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    val capabilities = state.capabilities

    SettingsPageLayout(
        title = stringResource(Res.string.settings_network_cache_title),
        onBack = onBack,
    ) {
        if (capabilities.networkStatusSupported || capabilities.backgroundScanSupported) {
            SettingsSection(title = stringResource(Res.string.settings_network_section)) {
                if (capabilities.networkStatusSupported) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_metered_streaming),
                        summary = stringResource(Res.string.settings_metered_streaming_summary),
                        checked = settings.allowMeteredStreaming,
                        onCheckedChange = { onAction(SettingsAction.SetAllowMeteredStreaming(it)) },
                    )
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_resume_network),
                        summary = stringResource(Res.string.settings_resume_network_summary),
                        checked = settings.resumePlaybackAfterNetworkRecovery,
                        onCheckedChange = {
                            onAction(SettingsAction.SetResumePlaybackAfterNetworkRecovery(it))
                        },
                    )
                }
                if (capabilities.backgroundScanSupported) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_background_sync_unmetered),
                        summary = stringResource(Res.string.settings_background_sync_unmetered_summary),
                        checked = settings.backgroundSyncOnlyOnUnmeteredNetwork,
                        onCheckedChange = {
                            onAction(SettingsAction.SetBackgroundSyncOnlyOnUnmeteredNetwork(it))
                        },
                    )
                }
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_audio_cache_section)) {
            CacheLimitChoices(
                currentBytes = settings.audioCacheLimitBytes,
                presets = AUDIO_CACHE_LIMIT_PRESETS_BYTES,
                type = CacheLimitType.Audio,
                onAction = onAction,
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_image_cache_section)) {
            CacheLimitChoices(
                currentBytes = settings.imageCacheLimitBytes,
                presets = IMAGE_CACHE_LIMIT_PRESETS_BYTES,
                type = CacheLimitType.Image,
                onAction = onAction,
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_advanced_section)) {
            if (capabilities.audioPreloadSupported) {
                listOf(2L, 4L, 8L).forEach { megabytes ->
                    val bytes = megabytes * BYTES_PER_MB
                    SettingsChoiceRow(
                        title = stringResource(Res.string.settings_audio_preload),
                        summary = stringResource(
                            Res.string.settings_audio_preload_summary,
                            formatBytes(bytes),
                        ),
                        selected = settings.audioPreloadBytes == bytes,
                        onClick = { onAction(SettingsAction.SetAudioPreloadBytes(bytes)) },
                    )
                }
            }
            listOf(10, 20, 30, 60).forEach { seconds ->
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_timeout),
                    summary = stringResource(Res.string.settings_timeout_value, seconds),
                    selected = settings.connectionTimeoutSeconds == seconds,
                    onClick = { onAction(SettingsAction.SetConnectionTimeoutSeconds(seconds)) },
                )
            }
            listOf(0, 1, 2, 3, 5).forEach { count ->
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_retry_count),
                    summary = stringResource(Res.string.settings_retry_count_value, count),
                    selected = settings.networkRetryCount == count,
                    onClick = { onAction(SettingsAction.SetNetworkRetryCount(count)) },
                )
            }
        }
    }

    SettingsInputDialog(
        show = state.customCacheLimitDialog != null,
        title = stringResource(Res.string.settings_custom_cache_title),
        message = stringResource(Res.string.settings_custom_cache_message),
        value = state.customCacheLimitInputMb,
        onValueChange = { onAction(SettingsAction.SetCustomCacheLimitInput(it)) },
        onConfirm = { onAction(SettingsAction.ApplyCustomCacheLimit) },
        onDismiss = { onAction(SettingsAction.DismissCustomCacheLimitDialog) },
    )
}

@Composable
private fun CacheLimitChoices(
    currentBytes: Long,
    presets: List<Long>,
    type: CacheLimitType,
    onAction: (SettingsAction) -> Unit,
) {
    presets.forEach { bytes ->
        SettingsChoiceRow(
            title = if (bytes == 0L) {
                stringResource(Res.string.settings_cache_disabled)
            } else {
                formatBytes(bytes)
            },
            summary = if (bytes == 0L) {
                stringResource(Res.string.settings_cache_disabled_summary)
            } else {
                stringResource(Res.string.settings_cache_limit_summary, formatBytes(bytes))
            },
            selected = currentBytes == bytes,
            onClick = {
                onAction(
                    when (type) {
                        CacheLimitType.Audio -> SettingsAction.SetAudioCacheLimitBytes(bytes)
                        CacheLimitType.Image -> SettingsAction.SetImageCacheLimitBytes(bytes)
                    }
                )
            },
        )
    }
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_cache_custom),
        summary = stringResource(Res.string.settings_cache_custom_summary, formatBytes(currentBytes)),
        selected = currentBytes !in presets,
        onClick = { onAction(SettingsAction.OpenCustomCacheLimitDialog(type)) },
    )
}

private const val BYTES_PER_MB = 1_048_576L
