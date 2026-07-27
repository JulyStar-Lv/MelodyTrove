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
                val preloadOptions = listOf(2L, 4L, 8L).map { it * BYTES_PER_MB }
                SettingsSelectRow(
                    label = stringResource(Res.string.settings_audio_preload),
                    selected = settings.audioPreloadBytes,
                    options = preloadOptions,
                    optionLabel = { bytes -> formatBytes(bytes) },
                    onSelect = { onAction(SettingsAction.SetAudioPreloadBytes(it)) },
                )
            }
            SettingsSelectRow(
                label = stringResource(Res.string.settings_timeout),
                selected = settings.connectionTimeoutSeconds,
                options = listOf(10, 20, 30, 60),
                optionLabel = { seconds ->
                    stringResource(Res.string.settings_timeout_value, seconds)
                },
                onSelect = { onAction(SettingsAction.SetConnectionTimeoutSeconds(it)) },
            )
            SettingsSelectRow(
                label = stringResource(Res.string.settings_retry_count),
                selected = settings.networkRetryCount,
                options = listOf(0, 1, 2, 3, 5),
                optionLabel = { count ->
                    stringResource(Res.string.settings_retry_count_value, count)
                },
                onSelect = { onAction(SettingsAction.SetNetworkRetryCount(it)) },
            )
        }
    }

    SettingsInputDialog(
        show = state.customCacheLimitDialog != null,
        title = stringResource(Res.string.settings_custom_cache_title),
        message = stringResource(Res.string.settings_custom_cache_message),
        value = state.customCacheLimitInputMb,
        label = stringResource(Res.string.settings_megabytes_unit),
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
    val isCustom = currentBytes !in presets
    SettingsSelectRow(
        label = stringResource(
            if (type == CacheLimitType.Audio) Res.string.settings_audio_cache_section
            else Res.string.settings_image_cache_section,
        ),
        selectedValue = if (isCustom) CACHE_CUSTOM_VALUE else currentBytes.toString(),
        selectedLabel = if (isCustom) {
            stringResource(Res.string.settings_cache_custom)
        } else {
            currentBytes.cacheLimitLabel()
        },
        options = presets.map { bytes ->
            SettingsSelectOption(value = bytes.toString(), label = bytes.cacheLimitLabel())
        } + SettingsSelectOption(
            value = CACHE_CUSTOM_VALUE,
            label = stringResource(Res.string.settings_cache_custom),
        ),
        onSelect = { value ->
            if (value == CACHE_CUSTOM_VALUE) {
                onAction(SettingsAction.OpenCustomCacheLimitDialog(type))
            } else {
                value.toLongOrNull()?.let { bytes ->
                    onAction(
                        when (type) {
                            CacheLimitType.Audio -> SettingsAction.SetAudioCacheLimitBytes(bytes)
                            CacheLimitType.Image -> SettingsAction.SetImageCacheLimitBytes(bytes)
                        }
                    )
                }
            }
        },
    )
}

@Composable
private fun Long.cacheLimitLabel(): String = if (this == 0L) {
    stringResource(Res.string.settings_cache_disabled)
} else {
    formatBytes(this)
}

private const val BYTES_PER_MB = 1_048_576L
private const val CACHE_CUSTOM_VALUE = "custom"
