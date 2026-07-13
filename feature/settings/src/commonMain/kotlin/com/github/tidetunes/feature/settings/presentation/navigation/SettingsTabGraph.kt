package com.github.tidetunes.feature.settings.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.settings.presentation.SettingsPage
import com.github.tidetunes.feature.settings.presentation.SettingsRoot

private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_APPEARANCE = "settings/appearance"
private const val ROUTE_PLAYBACK = "settings/playback"
private const val ROUTE_SOURCE = "settings/source"
private const val ROUTE_NETWORK_CACHE = "settings/network-cache"
private const val ROUTE_STORAGE = "settings/storage"
private const val ROUTE_ABOUT = "settings/about"
private const val ROUTE_LICENSES = "settings/licenses"

@Composable
fun SettingsTabGraph(
    navController: NavHostController,
    appVersion: String,
    appBuildInfo: String,
    gitCommitSha: String,
    onNavigateToLibraryFolderImport: () -> Unit,
) {
    fun navigate(route: String) {
        navController.navigate(route)
    }

    @Composable
    fun Route(page: SettingsPage) {
        SettingsRoot(
            page = page,
            appVersion = appVersion,
            appBuildInfo = appBuildInfo,
            gitCommitSha = gitCommitSha,
            onNavigateToAppearance = { navigate(ROUTE_APPEARANCE) },
            onNavigateToPlayback = { navigate(ROUTE_PLAYBACK) },
            onNavigateToSource = { navigate(ROUTE_SOURCE) },
            onNavigateToNetworkCache = { navigate(ROUTE_NETWORK_CACHE) },
            onNavigateToStorage = { navigate(ROUTE_STORAGE) },
            onNavigateToAbout = { navigate(ROUTE_ABOUT) },
            onNavigateToLicenses = { navigate(ROUTE_LICENSES) },
            onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
            onBack = { navController.navigateUp() },
        )
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_SETTINGS,
    ) {
        composable(ROUTE_SETTINGS) { Route(SettingsPage.Home) }
        composable(ROUTE_APPEARANCE) { Route(SettingsPage.Appearance) }
        composable(ROUTE_PLAYBACK) { Route(SettingsPage.Playback) }
        composable(ROUTE_SOURCE) { Route(SettingsPage.Source) }
        composable(ROUTE_NETWORK_CACHE) { Route(SettingsPage.NetworkCache) }
        composable(ROUTE_STORAGE) { Route(SettingsPage.Storage) }
        composable(ROUTE_ABOUT) { Route(SettingsPage.About) }
        composable(ROUTE_LICENSES) { Route(SettingsPage.Licenses) }
    }
}
