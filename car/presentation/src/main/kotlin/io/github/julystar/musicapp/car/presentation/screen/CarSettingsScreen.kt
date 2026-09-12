package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import io.github.julystar.musicapp.car.presentation.component.CarPreference
import io.github.julystar.musicapp.car.presentation.component.carInteractiveSurface
import io.github.julystar.musicapp.car.presentation.focus.CarFocusId
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as CarIconView
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.model.AppLanguageMode
import io.github.julystar.musicapp.core.domain.model.AppThemeMode
import io.github.julystar.musicapp.core.domain.model.AudioFocusMode
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CarSettingsScreen(
    metrics: CarLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val viewModel = koinViewModel<CarSettingsViewModel>()
    val uiState by viewModel.state.collectAsState()
    val settings = uiState.settings
    var selectedSection by rememberSaveable { mutableStateOf(CarSettingsSection.Playback) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap),
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = metrics.detailContentMargin,
                top = metrics.contentTop,
                end = metrics.detailContentEndMargin,
                bottom = LocalCarSpacing.current.wide,
            ),
    ) {
        Column(
            modifier = Modifier
                .width(metrics.detailHeroWidth)
                .fillMaxHeight()
                .clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = metrics.libraryPanePadding, vertical = 28.dp),
        ) {
            SettingsPanelTitle("设置")
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            SettingsGroupLabel("个性化")
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            SettingsGroupSurface {
                SettingsNavItem("外观与语言", CarIcon.Settings, CarSettingsSection.Appearance, selectedSection) { selectedSection = it }
                SettingsNavItem("歌词设置", CarIcon.Songs, CarSettingsSection.Lyrics, selectedSection) { selectedSection = it }
            }
            Spacer(Modifier.height(20.dp))
            SettingsGroupLabel("播放")
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            SettingsGroupSurface {
                SettingsNavItem("播放设置", CarIcon.Play, CarSettingsSection.Playback, selectedSection) { selectedSection = it }
            }
            Spacer(Modifier.height(20.dp))
            SettingsGroupLabel("音乐库与数据")
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            SettingsGroupSurface {
                SettingsNavItem("音源设置", CarIcon.Albums, CarSettingsSection.Source, selectedSection) { selectedSection = it }
                SettingsNavItem("元数据插件", CarIcon.Artists, CarSettingsSection.Metadata, selectedSection) { selectedSection = it }
                SettingsNavItem("网络与缓存", CarIcon.Search, CarSettingsSection.Network, selectedSection) { selectedSection = it }
                SettingsNavItem("存储与数据", CarIcon.Playlists, CarSettingsSection.Storage, selectedSection) { selectedSection = it }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 28.dp),
        ) {
            when (selectedSection) {
                CarSettingsSection.Appearance -> AppearanceSettings(
                    metrics = metrics,
                    settings = settings,
                    onAction = viewModel::onAction,
                    leftFocus = CarFocusIds.item("settings_nav", "Appearance"),
                )
                CarSettingsSection.Lyrics -> LyricsSettings(metrics, settings, viewModel::onAction)
                CarSettingsSection.Playback -> PlaybackSettings(
                    metrics = metrics,
                    settings = settings,
                    onAction = viewModel::onAction,
                    leftFocus = CarFocusIds.item("settings_nav", "Playback"),
                )
                CarSettingsSection.Source -> SourceSettings(
                    metrics = metrics,
                    state = uiState.localLibraryState,
                    onImport = { viewModel.onAction(CarSettingsAction.ImportLocalMusic) },
                    leftFocus = CarFocusIds.item("settings_nav", "Source"),
                )
                CarSettingsSection.Metadata -> MetadataSettings(metrics, settings)
                CarSettingsSection.Network -> NetworkSettings(metrics, settings, viewModel::onAction)
                CarSettingsSection.Storage -> StorageSettings(metrics, settings, viewModel::onAction)
            }
        }
    }
}

@Composable
private fun AppearanceSettings(
    metrics: CarLayoutMetrics,
    settings: AppSettings,
    onAction: (CarSettingsAction) -> Unit,
    leftFocus: CarFocusId,
) {
    SettingsPanelTitle("外观与语言")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("个性化")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        CarPreference(
            title = "深色模式",
            summary = when (settings.themeMode) {
                AppThemeMode.Dark -> "已开启"
                AppThemeMode.Light -> "已关闭"
                AppThemeMode.System -> "跟随系统"
            },
            checked = settings.themeMode == AppThemeMode.Dark,
            controlSize = metrics.primaryTouchTarget * 0.8125f,
            containerColor = Color.Transparent,
            shape = RectangleShape,
            highlightWhenChecked = false,
            onClick = { onAction(CarSettingsAction.ToggleTheme) },
            modifier = Modifier
                .carFocusTarget(CarFocusIds.item("setting", "theme"), left = leftFocus)
                .height(metrics.compactCardHeight),
        )
        CarPreference(
            title = "跟随封面配色",
            summary = "使用当前歌曲封面生成界面色彩",
            checked = settings.artworkThemeEnabled,
            controlSize = metrics.primaryTouchTarget * 0.8125f,
            containerColor = Color.Transparent,
            shape = RectangleShape,
            highlightWhenChecked = false,
            onClick = { onAction(CarSettingsAction.ToggleArtworkTheme) },
            modifier = Modifier.height(metrics.compactCardHeight),
        )
        CarPreference(
            title = "界面语言",
            summary = when (settings.languageMode) {
                AppLanguageMode.System -> "跟随系统"
                AppLanguageMode.Chinese -> "简体中文"
                AppLanguageMode.English -> "English"
            },
            controlSize = metrics.primaryTouchTarget * 0.8125f,
            containerColor = Color.Transparent,
            shape = RectangleShape,
            onClick = { onAction(CarSettingsAction.CycleLanguage) },
            modifier = Modifier.height(metrics.compactCardHeight),
        )
    }
}

@Composable
private fun LyricsSettings(
    metrics: CarLayoutMetrics,
    settings: AppSettings,
    onAction: (CarSettingsAction) -> Unit,
) {
    SettingsPanelTitle("歌词设置")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("显示与交互")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        SettingsToggle("显示翻译", settings.lyrics.showTranslation, metrics) {
            onAction(CarSettingsAction.ToggleLyricTranslation)
        }
        SettingsToggle("逐字歌词动效", settings.lyrics.wordLiftEnabled, metrics) {
            onAction(CarSettingsAction.ToggleLyricWordLift)
        }
        SettingsToggle("歌词模糊效果", settings.lyrics.blurEffectEnabled, metrics) {
            onAction(CarSettingsAction.ToggleLyricBlur)
        }
        SettingsToggle("点击歌词跳转", settings.lyrics.tapToSeekEnabled, metrics) {
            onAction(CarSettingsAction.ToggleLyricTapToSeek)
        }
    }
}

@Composable
private fun PlaybackSettings(
    metrics: CarLayoutMetrics,
    settings: AppSettings,
    onAction: (CarSettingsAction) -> Unit,
    leftFocus: CarFocusId,
) {
    val colors = LocalCarColors.current
    val switchWidth = metrics.primaryTouchTarget * 0.8125f
    SettingsPanelTitle("播放设置")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("音频输出")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        SettingsInfo("当前输出", "默认输出", metrics)
        SettingsInfo("选择输出设备", "跟随系统音频路由", metrics)
    }
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("音频焦点")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        SettingsInfo(
            title = "音频焦点处理",
            summary = when (settings.audioFocusMode) {
                AudioFocusMode.Pause -> "暂停播放"
                AudioFocusMode.Duck -> "降低音量"
                AudioFocusMode.Mix -> "允许混音"
            },
            metrics = metrics,
            onClick = { onAction(CarSettingsAction.CycleAudioFocus) },
        )
    }
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("播放行为")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
    CarPreference(
        title = "设备断开时暂停",
        checked = settings.pauseOnDisconnect,
        controlSize = switchWidth,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        highlightWhenChecked = false,
        onClick = { onAction(CarSettingsAction.TogglePauseOnDisconnect) },
        modifier = Modifier
            .carFocusTarget(
                CarFocusIds.item("setting", "pause_on_disconnect"),
                down = CarFocusIds.item("setting", "gapless"),
                left = leftFocus,
            )
            .height(metrics.compactCardHeight),
    )
    CarPreference(
        title = "无缝播放",
        checked = settings.gaplessPlaybackEnabled,
        controlSize = switchWidth,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        highlightWhenChecked = false,
        onClick = { onAction(CarSettingsAction.ToggleGaplessPlayback) },
        modifier = Modifier
            .carFocusTarget(
                CarFocusIds.item("setting", "gapless"),
                up = CarFocusIds.item("setting", "pause_on_disconnect"),
                down = CarFocusIds.item("setting", "retry"),
                left = leftFocus,
            )
            .height(metrics.compactCardHeight),
    )
    CarPreference(
        title = "播放失败后重试",
        checked = settings.retryPlaybackOnFailure,
        controlSize = switchWidth,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        highlightWhenChecked = false,
        onClick = { onAction(CarSettingsAction.TogglePlaybackRetry) },
        modifier = Modifier
            .carFocusTarget(
                CarFocusIds.item("setting", "retry"),
                up = CarFocusIds.item("setting", "gapless"),
                left = leftFocus,
            )
            .height(metrics.compactCardHeight),
    )
    CarPreference(
        title = "网络恢复后继续播放",
        checked = settings.resumePlaybackAfterNetworkRecovery,
        controlSize = switchWidth,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        highlightWhenChecked = false,
        onClick = { onAction(CarSettingsAction.ToggleResumeAfterNetworkRecovery) },
        modifier = Modifier.height(metrics.compactCardHeight),
    )
    }
}

@Composable
private fun SourceSettings(
    metrics: CarLayoutMetrics,
    state: CarLocalLibraryState,
    onImport: () -> Unit,
    leftFocus: CarFocusId,
) {
    SettingsPanelTitle("音源设置")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("本地音乐")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
    CarPreference(
        title = "扫描本地音乐",
        summary = state.summary,
        enabled = state != CarLocalLibraryState.Scanning,
        controlSize = metrics.primaryTouchTarget * 0.8125f,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        onClick = onImport,
        modifier = Modifier
            .carFocusTarget(CarFocusIds.item("setting", "local_music"), left = leftFocus)
            .height(metrics.compactCardHeight),
    )
    }
}

@Composable
private fun MetadataSettings(metrics: CarLayoutMetrics, settings: AppSettings) {
    SettingsPanelTitle("元数据插件")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("扫描与解析")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        SettingsInfo("标签解析", "优先读取文件内嵌元数据", metrics)
        SettingsInfo("封面与歌词", "允许插件补全缺失内容", metrics)
        SettingsInfo("WebDAV 扫描", settings.webDavMetadataScanMode.name, metrics)
    }
}

@Composable
private fun NetworkSettings(
    metrics: CarLayoutMetrics,
    settings: AppSettings,
    onAction: (CarSettingsAction) -> Unit,
) {
    SettingsPanelTitle("网络与缓存")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("网络")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        SettingsToggle("允许使用计费网络", settings.allowMeteredNetworkUsage, metrics) {
            onAction(CarSettingsAction.ToggleMeteredNetwork)
        }
        SettingsToggle("边听边缓存", settings.listenAndCacheEnabled, metrics) {
            onAction(CarSettingsAction.ToggleListenAndCache)
        }
        SettingsInfo("连接超时", "${settings.connectionTimeoutSeconds} 秒", metrics)
        SettingsInfo("音频缓存上限", settings.audioCacheLimitBytes.asStorageSize(), metrics)
        SettingsInfo("图片缓存上限", settings.imageCacheLimitBytes.asStorageSize(), metrics)
    }
}

@Composable
private fun StorageSettings(
    metrics: CarLayoutMetrics,
    settings: AppSettings,
    onAction: (CarSettingsAction) -> Unit,
) {
    SettingsPanelTitle("存储与数据")
    Spacer(Modifier.height(LocalCarSpacing.current.section))
    SettingsGroupLabel("音乐库")
    Spacer(Modifier.height(LocalCarSpacing.current.small))
    SettingsGroupSurface {
        SettingsToggle("扫描子目录", settings.scanSubdirectories, metrics) {
            onAction(CarSettingsAction.ToggleScanSubdirectories)
        }
        SettingsInfo("自动扫描", settings.autoScanMode.name, metrics)
        SettingsInfo("缺失文件策略", settings.missingFilePolicy.name, metrics)
        SettingsInfo("最低音频时长", "${settings.minimumAudioDurationMs / 1000} 秒", metrics)
    }
}

@Composable
private fun SettingsToggle(title: String, checked: Boolean, metrics: CarLayoutMetrics, onClick: () -> Unit) {
    CarPreference(
        title = title,
        checked = checked,
        controlSize = metrics.primaryTouchTarget * 0.8125f,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        highlightWhenChecked = false,
        onClick = onClick,
        modifier = Modifier.height(metrics.compactCardHeight),
    )
}

@Composable
private fun SettingsInfo(
    title: String,
    summary: String,
    metrics: CarLayoutMetrics,
    onClick: (() -> Unit)? = null,
) {
    CarPreference(
        title = title,
        summary = summary,
        interactive = onClick != null,
        controlSize = metrics.primaryTouchTarget * 0.8125f,
        containerColor = Color.Transparent,
        shape = RectangleShape,
        onClick = onClick ?: {},
        modifier = Modifier.height(metrics.compactCardHeight),
    )
}

@Composable
private fun SettingsNavItem(
    title: String,
    icon: CarIcon,
    section: CarSettingsSection,
    selected: CarSettingsSection,
    onSelect: (CarSettingsSection) -> Unit,
) {
    SettingsNavigationRow(
        title = title,
        icon = icon,
        selected = selected == section,
        onClick = { onSelect(section) },
        modifier = Modifier
            .carFocusTarget(
                id = CarFocusIds.item("settings_nav", section.name),
                left = CarFocusIds.Settings,
            )
            .height(88.dp),
    )
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    icon: CarIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .carInteractiveSurface(
                shape = RectangleShape,
                defaultColor = if (selected) colors.accentSubtle else Color.Transparent,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            CarIconView(
                icon = icon,
                contentDescription = null,
                tint = if (selected) colors.accentPrimary else colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(20.dp))
        BasicText(
            text = title,
            style = LocalCarTypography.current.title.copy(
                color = if (selected) colors.accentPrimary else colors.textPrimary,
            ),
        )
    }
}

@Composable
private fun SettingsGroupSurface(content: @Composable ColumnScope.() -> Unit) {
    val shape = LocalCarShapes.current.navigationItem
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LocalCarColors.current.panel),
        content = content,
    )
}

@Composable
private fun SettingsPanelTitle(title: String) {
    BasicText(
        text = title,
        style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary),
    )
}

@Composable
private fun SettingsGroupLabel(title: String) {
    BasicText(
        text = title,
        style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary),
    )
}

private enum class CarSettingsSection { Appearance, Lyrics, Playback, Source, Metadata, Network, Storage }

private fun Long.asStorageSize(): String = when {
    this >= 1024L * 1024L * 1024L -> "${this / (1024L * 1024L * 1024L)} GB"
    this >= 1024L * 1024L -> "${this / (1024L * 1024L)} MB"
    else -> "$this B"
}

private val CarLocalLibraryState.summary: String
    get() = when (this) {
        CarLocalLibraryState.Idle -> "读取并导入设备 Music 文件夹"
        CarLocalLibraryState.AwaitingPermission -> "请允许 Tide Player 访问音频文件"
        CarLocalLibraryState.Scanning -> "正在扫描 Music 文件夹…"
        is CarLocalLibraryState.Complete ->
            "已导入 ${result.importedCount} 首，跳过 ${result.skippedCount} 首，失败 ${result.failedCount} 首"
        is CarLocalLibraryState.Failed -> "扫描失败：$message"
    }
