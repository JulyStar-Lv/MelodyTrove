package com.github.tidetunes.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.core.presentation.navigation.NEW_STORAGE_ID
import com.github.tidetunes.core.presentation.navigation.routeIsHome

fun RouteHome(): MusicGraph.Home {
    return MusicGraph.Home
}

fun isRouteHome(route: String?): Boolean {
    return routeIsHome(route)
}

fun isRouteNowPlaying(route: String?): Boolean {
    return route != null && (route == "NowPlaying" || route.endsWith(".NowPlaying"))
}

fun RouteAddDevices(id: String): MusicGraph.EditStorage {
    return MusicGraph.EditStorage(id.toLongOrNull() ?: NEW_STORAGE_ID)
}

fun RouteImport(type: String): MusicGraph.Import {
    return MusicGraph.Import(type)
}

fun RouteMusicPlayer(): MusicGraph.NowPlaying {
    return MusicGraph.NowPlaying
}

val LocalNavController = compositionLocalOf<NavHostController> {
    error("No LocalNavController provided")
}

@Composable
fun RoutesProvider(
    block: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavController provides rememberNavController()) {
        block()
    }
}
