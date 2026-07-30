package io.github.julystar.musicapp.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import musicapp.shared.generated.resources.Res
import musicapp.shared.generated.resources.icon_nav_home
import musicapp.shared.generated.resources.icon_nav_library
import musicapp.shared.generated.resources.icon_nav_search
import musicapp.shared.generated.resources.icon_nav_settings
import musicapp.shared.generated.resources.nav_home
import musicapp.shared.generated.resources.nav_library
import musicapp.shared.generated.resources.nav_search
import musicapp.shared.generated.resources.nav_settings

enum class HomeTab(
    val index: Int,
    val painterRes: DrawableResource,
    val labelRes: StringResource,
) {
    HOME(0, Res.drawable.icon_nav_home, Res.string.nav_home),
    SEARCH(1, Res.drawable.icon_nav_search, Res.string.nav_search),
    LIBRARY(2, Res.drawable.icon_nav_library, Res.string.nav_library),
    SETTINGS(3, Res.drawable.icon_nav_settings, Res.string.nav_settings),
}
