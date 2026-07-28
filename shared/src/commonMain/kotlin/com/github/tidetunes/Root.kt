package com.github.tidetunes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.ToastRepository
import com.github.tidetunes.core.domain.recovery.StartupMode
import com.github.tidetunes.core.presentation.theme.TideTunesTheme
import com.github.tidetunes.core.presentation.theme.TideTunesThemeMode
import com.github.tidetunes.feature.home.presentation.HomeViewModel
import com.github.tidetunes.feature.home.presentation.LocalPreloadedHomeViewModel
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RoutesProvider
import com.github.tidetunes.navigation.AppNavigation
import com.github.tidetunes.platform.isSystemDynamicColorAvailable
import com.github.tidetunes.diagnostics.DiagnosticsBootstrapState
import com.github.tidetunes.diagnostics.RustDiagnosticsRepository
import com.github.tidetunes.diagnostics.SafeModeScreen
import com.github.tidetunes.core.domain.model.DiagnosticStartupStage
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Root(
    diagnosticsState: DiagnosticsBootstrapState? = null,
    onReady: () -> Unit = {},
    onStartupStable: () -> Unit = {},
    onTryNormalStartup: (Set<String>) -> Unit = {},
) {
    if (diagnosticsState?.safeMode == true) {
        SafeModeScreen(
            state = diagnosticsState,
            onTryNormalStartup = onTryNormalStartup,
        )
        StartupLifecycleEffect(onReady, onStartupStable)
        return
    }
    RoutesProvider {
        val controller = LocalNavController.current
        val settingsRepository = koinInject<SettingsRepository>()
        val toastRepository = koinInject<ToastRepository>()
        val settings by settingsRepository.settings.collectAsState<AppSettings, AppSettings?>(null)
        val homeViewModel = koinViewModel<HomeViewModel>()
        val homeState by homeViewModel.state.collectAsState()
        val loadedSettings = settings
        if (loadedSettings == null || homeState.isLoading) {
            AppStartupScreen()
            return@RoutesProvider
        }

        TideTunesTheme(
            themeMode = loadedSettings.themeMode.toPresentationThemeMode(),
            dynamicColor = loadedSettings.dynamicColorEnabled && isSystemDynamicColorAvailable(),
        ) {
            CompositionLocalProvider(LocalPreloadedHomeViewModel provides homeViewModel) {
                AppNavigation(navController = controller)
            }
        }
        LaunchedEffect(diagnosticsState?.startupPlan) {
            diagnosticsState?.startupPlan
                ?.takeIf { it.mode != StartupMode.NormalStartup }
                ?.reason
                ?.let(toastRepository::emitToast)
        }
        StartupLifecycleEffect(onReady, onStartupStable)
    }
}

@Composable
private fun StartupLifecycleEffect(
    onReady: () -> Unit,
    onStartupStable: () -> Unit,
) {
    LaunchedEffect(Unit) {
        runCatching {
            RustDiagnosticsRepository.updateStartupStage(DiagnosticStartupStage.UiCompositionStarted)
        }
        withFrameNanos { }
        runCatching {
            RustDiagnosticsRepository.updateStartupStage(DiagnosticStartupStage.FirstFrameRendered)
        }
        onReady()
        delay(10_000)
        runCatching {
            RustDiagnosticsRepository.updateStartupStage(DiagnosticStartupStage.StartupStable)
        }
        onStartupStable()
    }
}

@Composable
private fun AppStartupScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F5FC)),
    )
}

private fun AppThemeMode.toPresentationThemeMode(): TideTunesThemeMode {
    return when (this) {
        AppThemeMode.System -> TideTunesThemeMode.FollowSystem
        AppThemeMode.Light -> TideTunesThemeMode.Light
        AppThemeMode.Dark -> TideTunesThemeMode.Dark
    }
}
