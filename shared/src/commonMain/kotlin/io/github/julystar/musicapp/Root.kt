package io.github.julystar.musicapp

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
import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.model.AppThemeMode as DomainAppThemeMode
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import io.github.julystar.musicapp.core.domain.repository.ToastRepository
import io.github.julystar.musicapp.core.domain.recovery.StartupMode
import io.github.julystar.musicapp.core.presentation.theme.AppTheme
import io.github.julystar.musicapp.core.presentation.theme.AppThemeMode as PresentationAppThemeMode
import io.github.julystar.musicapp.feature.home.presentation.HomeViewModel
import io.github.julystar.musicapp.feature.home.presentation.LocalPreloadedHomeViewModel
import io.github.julystar.musicapp.core.LocalNavController
import io.github.julystar.musicapp.core.RoutesProvider
import io.github.julystar.musicapp.navigation.AppNavigation
import io.github.julystar.musicapp.platform.isSystemDynamicColorAvailable
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrapState
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.diagnostics.SafeModeScreen
import io.github.julystar.musicapp.core.domain.model.DiagnosticStartupStage
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

        AppTheme(
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

private fun DomainAppThemeMode.toPresentationThemeMode(): PresentationAppThemeMode {
    return when (this) {
        DomainAppThemeMode.System -> PresentationAppThemeMode.FollowSystem
        DomainAppThemeMode.Light -> PresentationAppThemeMode.Light
        DomainAppThemeMode.Dark -> PresentationAppThemeMode.Dark
    }
}
