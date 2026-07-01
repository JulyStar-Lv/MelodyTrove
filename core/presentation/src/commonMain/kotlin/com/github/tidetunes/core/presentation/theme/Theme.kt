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
    themeMode: TideTunesThemeMode = TideTunesThemeMode.FollowSystem,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = when (themeMode) {
        TideTunesThemeMode.FollowSystem -> ColorSchemeMode.System
        TideTunesThemeMode.Light -> ColorSchemeMode.Light
        TideTunesThemeMode.Dark -> ColorSchemeMode.Dark
    }
    val controller = remember(colorSchemeMode, darkTheme) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            lightColors = TideTunesLightColors,
            darkColors = TideTunesDarkColors,
            isDark = darkTheme,
        )
    }
    val textStyles = remember { tideTunesTextStyles() }

    MiuixTheme(
        controller = controller,
        textStyles = textStyles,
    ) {
        CompositionLocalProvider(
            LocalTideTunesSpacing provides TideTunesSpacing(),
            LocalTideTunesShapes provides TideTunesShapes(),
            LocalTideTunesMotion provides TideTunesMotion(),
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
)

@Immutable
data class TideTunesShapes(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 28.dp,
    val full: Dp = 999.dp,
)

@Immutable
data class TideTunesMotion(
    val instantMillis: Int = 0,
    val fastMillis: Int = 100,
    val standardMillis: Int = 180,
    val emphasizedMillis: Int = 280,
    val playerExpandMillis: Int = 380,
)

private val LocalTideTunesSpacing = staticCompositionLocalOf { TideTunesSpacing() }
private val LocalTideTunesShapes = staticCompositionLocalOf { TideTunesShapes() }
private val LocalTideTunesMotion = staticCompositionLocalOf { TideTunesMotion() }
