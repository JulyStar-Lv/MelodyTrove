package io.github.julystar.musicapp.car.presentation.navigation

import io.github.julystar.musicapp.car.presentation.icon.CarIcon

enum class CarRoute(val label: String, val icon: CarIcon) {
    Home("首页", CarIcon.Home),
    Playlists("歌单", CarIcon.Playlists),
    Settings("设置", CarIcon.Settings),
    Songs("歌曲", CarIcon.Songs),
    Albums("专辑", CarIcon.Albums),
    Artists("歌手", CarIcon.Artists),
    Search("搜索", CarIcon.Search),
    NowPlaying("正在播放", CarIcon.Play),
}
