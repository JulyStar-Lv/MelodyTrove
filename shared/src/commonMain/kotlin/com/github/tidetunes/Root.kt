package com.github.tidetunes

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.presentation.theme.TideTunesTheme
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.RoutesProvider
import com.github.tidetunes.navigation.AppNavigation

@Composable
fun Root() {
    RoutesProvider {
        val controller = LocalNavController.current

        TideTunesTheme {
            AppNavigation(navController = controller)
        }
    }
}
