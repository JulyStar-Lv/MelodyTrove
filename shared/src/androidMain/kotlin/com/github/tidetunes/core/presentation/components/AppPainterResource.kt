package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource as androidPainterResource
import com.github.tidetunes.shared.R
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource as composePainterResource
import tidetunes.shared.generated.resources.*

@Composable
actual fun appPainterResource(resource: DrawableResource): Painter {
    val androidResourceId = androidDrawableResourceId(resource)
    return if (androidResourceId == null) {
        composePainterResource(resource)
    } else {
        androidPainterResource(androidResourceId)
    }
}

private fun androidDrawableResourceId(resource: DrawableResource): Int? = when (resource) {
    Res.drawable.empty_playlist -> R.drawable.empty_playlist
    Res.drawable.empty_playlists -> R.drawable.empty_playlists
    Res.drawable.ic_launcher_background -> R.drawable.ic_launcher_background
    Res.drawable.ic_launcher_foreground -> R.drawable.ic_launcher_foreground
    Res.drawable.icon_adjust -> R.drawable.icon_adjust
    Res.drawable.icon_album -> R.drawable.icon_album
    Res.drawable.icon_back -> R.drawable.icon_back
    Res.drawable.icon_cloud -> R.drawable.icon_cloud
    Res.drawable.icon_collapse -> R.drawable.icon_collapse
    Res.drawable.icon_dashboard -> R.drawable.icon_dashboard
    Res.drawable.icon_deleteseep -> R.drawable.icon_deleteseep
    Res.drawable.icon_download -> R.drawable.icon_download
    Res.drawable.icon_drag -> R.drawable.icon_drag
    Res.drawable.icon_file -> R.drawable.icon_file
    Res.drawable.icon_folder -> R.drawable.icon_folder
    Res.drawable.icon_github -> R.drawable.icon_github
    Res.drawable.icon_image -> R.drawable.icon_image
    Res.drawable.icon_info -> R.drawable.icon_info
    Res.drawable.icon_log -> R.drawable.icon_log
    Res.drawable.icon_lyrics -> R.drawable.icon_lyrics
    Res.drawable.icon_mode_list -> R.drawable.icon_mode_list
    Res.drawable.icon_mode_one -> R.drawable.icon_mode_one
    Res.drawable.icon_mode_repeat -> R.drawable.icon_mode_repeat
    Res.drawable.icon_mode_repeatone -> R.drawable.icon_mode_repeatone
    Res.drawable.icon_music_note -> R.drawable.icon_music_note
    Res.drawable.icon_ok -> R.drawable.icon_ok
    Res.drawable.icon_onedrive -> R.drawable.icon_onedrive
    Res.drawable.icon_pause -> R.drawable.icon_pause
    Res.drawable.icon_play -> R.drawable.icon_play
    Res.drawable.icon_play_next -> R.drawable.icon_play_next
    Res.drawable.icon_play_previous -> R.drawable.icon_play_previous
    Res.drawable.icon_plus -> R.drawable.icon_plus
    Res.drawable.icon_search -> R.drawable.icon_search
    Res.drawable.icon_setting -> R.drawable.icon_setting
    Res.drawable.icon_stop -> R.drawable.icon_stop
    Res.drawable.icon_timelapse -> R.drawable.icon_timelapse
    Res.drawable.icon_toggle_all -> R.drawable.icon_toggle_all
    Res.drawable.icon_vertialcal_more -> R.drawable.icon_vertialcal_more
    Res.drawable.icon_visibility -> R.drawable.icon_visibility
    Res.drawable.icon_visibility_off -> R.drawable.icon_visibility_off
    Res.drawable.icon_warning -> R.drawable.icon_warning
    Res.drawable.icon_wifitethering -> R.drawable.icon_wifitethering
    Res.drawable.icon_yes -> R.drawable.icon_yes
    else -> null
}
