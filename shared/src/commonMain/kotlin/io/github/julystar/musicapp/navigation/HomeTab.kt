package io.github.julystar.musicapp.navigation

import org.jetbrains.compose.resources.DrawableResource
import musicapp.shared.generated.resources.Res
import musicapp.shared.generated.resources.icon_nav_home
import musicapp.shared.generated.resources.icon_nav_library
import musicapp.shared.generated.resources.icon_nav_search
import musicapp.shared.generated.resources.icon_nav_settings

enum class HomeTab(
    val index: Int,
    val painterRes: DrawableResource,
    val label: String,
) {
    HOME(0, Res.drawable.icon_nav_home, "Home"),
    SEARCH(1, Res.drawable.icon_nav_search, "Search"),
    LIBRARY(2, Res.drawable.icon_nav_library, "Library"),
    SETTINGS(3, Res.drawable.icon_nav_settings, "Settings"),
}
