package com.github.tidetunes.navigation

import org.jetbrains.compose.resources.DrawableResource
import tidetunes.shared.generated.resources.Res
import tidetunes.shared.generated.resources.icon_album
import tidetunes.shared.generated.resources.icon_dashboard
import tidetunes.shared.generated.resources.icon_music_note
import tidetunes.shared.generated.resources.icon_search
import tidetunes.shared.generated.resources.icon_setting

enum class HomeTab(
    val index: Int,
    val painterRes: DrawableResource,
    val label: String,
) {
    PLAYLISTS(0, Res.drawable.icon_album, "Playlists"),
    LIBRARY(1, Res.drawable.icon_music_note, "Library"),
    SEARCH(2, Res.drawable.icon_search, "Search"),
    DASHBOARD(3, Res.drawable.icon_dashboard, "Dashboard"),
    SETTINGS(4, Res.drawable.icon_setting, "Settings"),
}
