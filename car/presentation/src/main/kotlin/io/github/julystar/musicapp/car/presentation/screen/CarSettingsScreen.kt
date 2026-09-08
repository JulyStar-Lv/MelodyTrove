package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.car.presentation.component.CarPreference
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.model.AppThemeMode
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun CarSettingsScreen(
    metrics: CarLayoutMetrics,
    settings: AppSettings,
    repository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
        contentPadding = PaddingValues(
            start = metrics.contentHorizontalPadding,
            top = metrics.contentTop,
            end = metrics.contentHorizontalPadding,
            bottom = LocalCarSpacing.current.wide,
        ),
        modifier = modifier,
    ) {
        item { CarSectionTitle("设置") }
        item {
            CarPreference(
                title = "深色模式",
                summary = when (settings.themeMode) {
                    AppThemeMode.Dark -> "已开启"
                    AppThemeMode.Light -> "已关闭"
                    AppThemeMode.System -> "跟随系统"
                },
                checked = settings.themeMode == AppThemeMode.Dark,
                controlSize = metrics.iconSize,
                onClick = {
                    scope.launch {
                        repository.setThemeMode(
                            if (settings.themeMode == AppThemeMode.Dark) AppThemeMode.Light else AppThemeMode.Dark,
                        )
                    }
                },
                modifier = Modifier.height(metrics.compactCardHeight),
            )
        }
        item {
            CarPreference(
                title = "设备断开时暂停",
                summary = "蓝牙或音频输出断开后暂停播放",
                checked = settings.pauseOnDisconnect,
                controlSize = metrics.iconSize,
                onClick = { scope.launch { repository.setPauseOnDisconnect(!settings.pauseOnDisconnect) } },
                modifier = Modifier.height(metrics.compactCardHeight),
            )
        }
        item {
            CarPreference(
                title = "无缝播放",
                summary = "支持时减少曲目切换间隔",
                checked = settings.gaplessPlaybackEnabled,
                controlSize = metrics.iconSize,
                onClick = { scope.launch { repository.setGaplessPlaybackEnabled(!settings.gaplessPlaybackEnabled) } },
                modifier = Modifier.height(metrics.compactCardHeight),
            )
        }
        item {
            CarPreference(
                title = "播放失败后重试",
                summary = "临时网络错误时自动重新尝试",
                checked = settings.retryPlaybackOnFailure,
                controlSize = metrics.iconSize,
                onClick = { scope.launch { repository.setRetryPlaybackOnFailure(!settings.retryPlaybackOnFailure) } },
                modifier = Modifier.height(metrics.compactCardHeight),
            )
        }
    }
}
