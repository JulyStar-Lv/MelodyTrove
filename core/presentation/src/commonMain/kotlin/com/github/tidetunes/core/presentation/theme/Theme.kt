package com.github.tidetunes.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.platform.SystemBarsEffect
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

enum class TideTunesThemeMode {
    FollowSystem,
    Light,
    Dark,
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun TideTunesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeMode: TideTunesThemeMode = TideTunesThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = when (themeMode) {
        TideTunesThemeMode.FollowSystem -> ColorSchemeMode.System
        TideTunesThemeMode.Light -> ColorSchemeMode.Light
        TideTunesThemeMode.Dark -> ColorSchemeMode.Dark
    }
    val effectiveDarkTheme = when (themeMode) {
        TideTunesThemeMode.FollowSystem -> darkTheme
        TideTunesThemeMode.Light -> false
        TideTunesThemeMode.Dark -> true
    }
    val controller = remember(colorSchemeMode, effectiveDarkTheme) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            lightColors = TideTunesLightColors,
            darkColors = TideTunesDarkColors,
            isDark = effectiveDarkTheme,
        )
    }
    val textStyles = tideTunesTextStyles()
    SystemBarsEffect(isDarkTheme = effectiveDarkTheme)

    MiuixTheme(
        controller = controller,
        textStyles = textStyles,
    ) {
        CompositionLocalProvider(
            LocalTideTunesSpacing provides TideTunesSpacing(),
            LocalTideTunesShapes provides TideTunesShapes(),
            LocalTideTunesMotion provides TideTunesMotion(),
            LocalTideTunesBlur provides TideTunesBlur(),
            LocalTideTunesElevation provides TideTunesElevation(),
            LocalTideTunesAdaptive provides TideTunesAdaptive(),
            LocalTideTunesNavigation provides TideTunesNavigation(),
            LocalTideTunesPlayer provides TideTunesPlayer(),
            content = content,
        )
    }
}

object TideTunesTokens {
    val spacing: TideTunesSpacing
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesSpacing.current

    val shapes: TideTunesShapes
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesShapes.current

    val motion: TideTunesMotion
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesMotion.current

    val blur: TideTunesBlur
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesBlur.current

    val elevation: TideTunesElevation
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesElevation.current

    val adaptive: TideTunesAdaptive
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesAdaptive.current

    val navigation: TideTunesNavigation
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesNavigation.current

    val player: TideTunesPlayer
        @Composable @ReadOnlyComposable
        get() = LocalTideTunesPlayer.current
}

@Immutable
data class TideTunesSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val section: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val huge: Dp = 40.dp,
    val xxl: Dp = 48.dp,
    val pageCompact: Dp = 16.dp,
    val pageMedium: Dp = 20.dp,
    val pageExpanded: Dp = 24.dp,
)

@Immutable
data class TideTunesShapes(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val compactCard: Dp = 18.dp,
    val md: Dp = 20.dp,
    val card: Dp = 24.dp,
    val lg: Dp = 28.dp,
    val xl: Dp = 36.dp,
    val xxl: Dp = 40.dp,
    val full: Dp = 999.dp,
)

@Immutable
data class TideTunesMotion(
    val instantMillis: Int = 100,
    val fastMillis: Int = 180,
    val standardMillis: Int = 280,
    val emphasizedMillis: Int = 380,
    val morphMillis: Int = 500,
    val themeMillis: Int = 240,
    val playerExpandMillis: Int = 380,
)

@Immutable
data class TideTunesBlur(
    val none: Dp = 0.dp,
    val light: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val heavy: Dp = 32.dp,
    val ultra: Dp = 48.dp,
)

@Immutable
data class TideTunesElevation(
    val surface: Dp = 0.dp,
    val card: Dp = 2.dp,
    val popup: Dp = 12.dp,
    val floating: Dp = 20.dp,
    val overlay: Dp = 28.dp,
)

@Immutable
data class TideTunesAdaptive(
    val compactMaxWidth: Dp = 599.dp,
    val mediumMaxWidth: Dp = 839.dp,
    val expandedMaxWidth: Dp = 1279.dp,
    val largeMinWidth: Dp = 1280.dp,
    val extraLargeMinWidth: Dp = 1600.dp,
    val contentMaxWidth: Dp = 1180.dp,
    val detailMaxWidth: Dp = 720.dp,
    val sidebarWidth: Dp = 224.dp,
    val railWidth: Dp = 80.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val compactHeaderCollapseDistance: Dp = 48.dp,
    val compactHeaderHeight: Dp = 58.dp,
)

@Immutable
data class TideTunesNavigation(
    val compactBarHeight: Dp = 62.dp,
    val compactBarDividerHeight: Dp = 1.dp,
    val compactSelectedIndicatorWidth: Dp = 48.dp,
    val compactSelectedIndicatorHeight: Dp = 28.dp,
    val compactIconSize: Dp = 20.dp,
    val compactLabelSize: TextUnit = 10.sp,
)

@Immutable
data class TideTunesPlayer(
    val miniBarHeight: Dp = 72.dp,
    val compactMiniBarHeight: Dp = 76.dp,
)

private val LocalTideTunesSpacing = staticCompositionLocalOf { TideTunesSpacing() }
private val LocalTideTunesShapes = staticCompositionLocalOf { TideTunesShapes() }
private val LocalTideTunesMotion = staticCompositionLocalOf { TideTunesMotion() }
private val LocalTideTunesBlur = staticCompositionLocalOf { TideTunesBlur() }
private val LocalTideTunesElevation = staticCompositionLocalOf { TideTunesElevation() }
private val LocalTideTunesAdaptive = staticCompositionLocalOf { TideTunesAdaptive() }
private val LocalTideTunesNavigation = staticCompositionLocalOf { TideTunesNavigation() }
private val LocalTideTunesPlayer = staticCompositionLocalOf { TideTunesPlayer() }
