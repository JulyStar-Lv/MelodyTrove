package com.github.tidetunes.navigation

import org.jetbrains.compose.resources.DrawableResource
import tidetunes.shared.generated.resources.Res
import tidetunes.shared.generated.resources.icon_dashboard
import tidetunes.shared.generated.resources.icon_music_note
import tidetunes.shared.generated.resources.icon_search
import tidetunes.shared.generated.resources.icon_setting

enum class HomeTab(
    val index: Int,
    val painterRes: DrawableResource,
    val label: String,
) {
    HOME(0, Res.drawable.icon_dashboard, "Home"),
    SEARCH(1, Res.drawable.icon_search, "Search"),
    LIBRARY(2, Res.drawable.icon_music_note, "Library"),
    SETTINGS(3, Res.drawable.icon_setting, "Settings"),
}
