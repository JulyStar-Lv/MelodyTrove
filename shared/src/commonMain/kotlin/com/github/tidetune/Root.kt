package com.github.tidetune

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.github.tidetune.ui.theme.TideTuneTheme
import com.github.tidetune.core.LocalNavController
import com.github.tidetune.core.RouteAddDevices
import com.github.tidetune.core.RouteDebugMore
import com.github.tidetune.core.RouteHome
import com.github.tidetune.core.RouteImport
import com.github.tidetune.core.RouteLog
import com.github.tidetune.core.RouteMusicPlayer
import com.github.tidetune.core.RoutePlaylist
import com.github.tidetune.core.RoutesProvider
import com.github.tidetune.viewmodels.EditStorageVM
import com.github.tidetune.widgets.ToastFrame
import com.github.tidetune.widgets.dashboard.TimeToPauseModal
import com.github.tidetune.widgets.devices.EditStoragesPage
import com.github.tidetune.widgets.home.HomePage
import com.github.tidetune.widgets.musics.ImportMusicsPage
import com.github.tidetune.widgets.musics.MusicPlayerPage
import com.github.tidetune.widgets.playlists.CreatePlaylistsDialog
import com.github.tidetune.widgets.playlists.EditPlaylistsDialog
import com.github.tidetune.widgets.playlists.PlaylistPage
import com.github.tidetune.widgets.settings.DebugMorePage
import com.github.tidetune.widgets.settings.LogPage

@Composable
fun Root() {
    RoutesProvider {
        val controller = LocalNavController.current

        TideTuneTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
            ) { scaffoldPadding ->
                Box(
                    modifier = Modifier
                        .padding(
                            start = scaffoldPadding.calculateLeftPadding(LayoutDirection.Ltr),
                            end = scaffoldPadding.calculateRightPadding(LayoutDirection.Ltr),
                            top = scaffoldPadding.calculateTopPadding(),
                        )
                        .fillMaxSize()
                ) {
                    NavHost(
                        modifier = Modifier
                            .fillMaxSize(),
                        navController = controller,
                        startDestination = RouteHome(),
                        enterTransition = {
                            slideIn(
                                animationSpec = tween(300),
                                initialOffset = { fullSize ->
                                    IntOffset(fullSize.width, 0)
                                })
                        },
                        exitTransition = {
                            slideOut(
                                animationSpec = tween(300),
                                targetOffset = { fullSize ->
                                    IntOffset(-fullSize.width, 0)
                                })
                        },
                        popEnterTransition = {
                            slideIn(
                                animationSpec = tween(300),
                                initialOffset = { fullSize ->
                                    IntOffset(fullSize.width, 0)
                                })
                        },
                        popExitTransition = {
                            slideOut(
                                animationSpec = tween(300),
                                targetOffset = { fullSize ->
                                    IntOffset(-fullSize.width, 0)
                                })
                        },
                    ) {
                        composable(RouteHome()) {
                            HomePage(
                                scaffoldPadding = scaffoldPadding,
                            )
                            CreatePlaylistsDialog()
                            TimeToPauseModal()
                        }
                        composable(
                            RouteAddDevices("{id}"),
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) {
                            EditStoragesPage()
                        }
                        composable(
                            RoutePlaylist("{id}"),
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) {
                            PlaylistPage(
                                scaffoldPadding = scaffoldPadding,
                            )
                            EditPlaylistsDialog()
                        }
                        composable(
                            RouteImport("{type}"),
                            arguments = listOf(navArgument("type") { type = NavType.StringType } )
                        ){
                            ImportMusicsPage()
                        }
                        composable(RouteMusicPlayer()) {
                            MusicPlayerPage()
                            TimeToPauseModal()
                        }
                        composable(RouteLog()) {
                            LogPage()
                        }
                        composable(RouteDebugMore()) {
                            DebugMorePage()
                        }
                    }
                    ToastFrame()
                }
            }
        }
    }
}