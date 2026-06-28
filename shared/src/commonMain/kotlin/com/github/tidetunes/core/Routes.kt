package com.github.tidetunes.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.tidetunes.navigation.MusicGraph
import com.github.tidetunes.navigation.NEW_STORAGE_ID
import com.github.tidetunes.navigation.routeIsHome

fun RouteHome(): MusicGraph.Home {
    return MusicGraph.Home
}

fun isRouteHome(route: String?): Boolean {
    return routeIsHome(route)
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

fun RouteLog(): MusicGraph.Log {
    return MusicGraph.Log
}

fun RouteDebugMore(): MusicGraph.DebugMore {
    return MusicGraph.DebugMore
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
