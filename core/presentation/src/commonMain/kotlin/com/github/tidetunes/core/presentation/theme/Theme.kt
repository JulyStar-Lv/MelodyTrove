package com.github.tidetunes.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

    MiuixTheme(
        controller = controller,
        textStyles = textStyles,
    ) {
        CompositionLocalProvider(
            LocalTideTunesSpacing provides TideTunesSpacing(),
            LocalTideTunesShapes provides TideTunesShapes(),
            LocalTideTunesMotion provides TideTunesMotion(),
            LocalTideTunesBlur provides TideTunesBlur(),
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
}

@Immutable
data class TideTunesSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
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
    val md: Dp = 20.dp,
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

private val LocalTideTunesSpacing = staticCompositionLocalOf { TideTunesSpacing() }
private val LocalTideTunesShapes = staticCompositionLocalOf { TideTunesShapes() }
private val LocalTideTunesMotion = staticCompositionLocalOf { TideTunesMotion() }
private val LocalTideTunesBlur = staticCompositionLocalOf { TideTunesBlur() }
