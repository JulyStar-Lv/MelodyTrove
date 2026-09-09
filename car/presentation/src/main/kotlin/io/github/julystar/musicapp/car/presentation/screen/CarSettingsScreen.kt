package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.car.presentation.component.CarPreference
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.CarFocusCoordinator
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
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
    focusCoordinator: CarFocusCoordinator,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap),
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = metrics.detailContentMargin,
                top = metrics.contentTop,
                end = metrics.detailContentMargin,
                bottom = LocalCarSpacing.current.wide,
            ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
            modifier = Modifier.width(metrics.detailHeroWidth).fillMaxHeight(),
        ) {
            CarSectionTitle("设置")
            CarPreference(
                title = "播放与外观",
                summary = "主题、音频输出与播放恢复",
                controlSize = metrics.iconSize,
                onClick = { focusCoordinator.requestFocus(CarFocusIds.item("setting", "theme")) },
                modifier = Modifier
                    .carFocusTarget(
                        CarFocusIds.content("Settings"),
                        left = CarFocusIds.Settings,
                        right = CarFocusIds.item("setting", "theme"),
                    )
                    .height(metrics.compactCardHeight),
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
            contentPadding = PaddingValues(bottom = LocalCarSpacing.current.wide),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            item { CarSectionTitle("播放与外观") }
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
                    modifier = Modifier
                        .carFocusTarget(
                            CarFocusIds.item("setting", "theme"),
                            left = CarFocusIds.content("Settings"),
                        )
                        .height(metrics.compactCardHeight),
                )
            }
            item {
                CarPreference(
                    title = "设备断开时暂停",
                    summary = "蓝牙或音频输出断开后暂停播放",
                    checked = settings.pauseOnDisconnect,
                    controlSize = metrics.iconSize,
                    onClick = { scope.launch { repository.setPauseOnDisconnect(!settings.pauseOnDisconnect) } },
                    modifier = Modifier
                        .carFocusTarget(
                            CarFocusIds.item("setting", "pause_on_disconnect"),
                            left = CarFocusIds.content("Settings"),
                        )
                        .height(metrics.compactCardHeight),
                )
            }
            item {
                CarPreference(
                    title = "无缝播放",
                    summary = "支持时减少曲目切换间隔",
                    checked = settings.gaplessPlaybackEnabled,
                    controlSize = metrics.iconSize,
                    onClick = { scope.launch { repository.setGaplessPlaybackEnabled(!settings.gaplessPlaybackEnabled) } },
                    modifier = Modifier
                        .carFocusTarget(
                            CarFocusIds.item("setting", "gapless"),
                            left = CarFocusIds.content("Settings"),
                        )
                        .height(metrics.compactCardHeight),
                )
            }
            item {
                CarPreference(
                    title = "播放失败后重试",
                    summary = "临时网络错误时自动重新尝试",
                    checked = settings.retryPlaybackOnFailure,
                    controlSize = metrics.iconSize,
                    onClick = { scope.launch { repository.setRetryPlaybackOnFailure(!settings.retryPlaybackOnFailure) } },
                    modifier = Modifier
                        .carFocusTarget(
                            CarFocusIds.item("setting", "retry"),
                            left = CarFocusIds.content("Settings"),
                        )
                        .height(metrics.compactCardHeight),
                )
            }
        }
    }
}
