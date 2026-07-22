package com.github.tidetunes.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.isRouteNowPlaying
import com.github.tidetunes.widgets.ToastFrame
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
internal fun AppShell(
    content: @Composable (PaddingValues) -> Unit,
) {
    val navController = LocalNavController.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val drawsBehindStatusBar = isRouteNowPlaying(currentBackStackEntry?.destination?.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(
                    start = scaffoldPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = scaffoldPadding.calculateRightPadding(LayoutDirection.Ltr),
                    top = if (drawsBehindStatusBar) {
                        0.dp
                    } else {
                        scaffoldPadding.calculateTopPadding()
                    },
                )
                .fillMaxSize(),
        ) {
            content(scaffoldPadding)
            ToastFrame()
        }
    }
}
