package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoot(
    page: SettingsPage,
    appVersion: String,
    appBuildInfo: String,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToSource: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToLibraryFolderImport: () -> Unit,
    onBack: () -> Unit,
    settingsVM: SettingsVM = koinViewModel(),
) {
    val state by settingsVM.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(settingsVM) {
        settingsVM.eventFlow.collect { event ->
            when (event) {
                SettingsEvent.OpenLibraryFolderImport -> onNavigateToLibraryFolderImport()
            }
        }
    }

    when (page) {
        SettingsPage.Home -> SettingsScreen(
            state = state,
            onAction = settingsVM::onAction,
            onNavigateToAppearance = onNavigateToAppearance,
            onNavigateToPlayback = onNavigateToPlayback,
            onNavigateToSource = onNavigateToSource,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToAbout = onNavigateToAbout,
        )
        SettingsPage.Appearance -> AppearanceSettingsSection(
            state = state,
            onBack = onBack,
            onAction = settingsVM::onAction,
        )
        SettingsPage.Playback -> PlaybackSettingsSection(
            state = state,
            onBack = onBack,
            onAction = settingsVM::onAction,
        )
        SettingsPage.Source -> SourceSettingsSection(
            state = state,
            onBack = onBack,
            onAction = settingsVM::onAction,
        )
        SettingsPage.Storage -> StorageSettingsSection(
            state = state,
            onBack = onBack,
            onAction = settingsVM::onAction,
        )
        SettingsPage.About -> AboutSettingsSection(
            appVersion = appVersion,
            appBuildInfo = appBuildInfo,
            onBack = onBack,
            onOpenLicenses = onNavigateToLicenses,
            onOpenRepository = { uriHandler.openUri(TIDE_TUNES_REPOSITORY_URL) },
        )
        SettingsPage.Licenses -> LicensesSettingsScreen(onBack = onBack)
    }
}
