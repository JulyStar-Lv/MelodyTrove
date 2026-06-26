package com.github.tidetune.widgets.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import com.github.tidetune.viewmodels.PlayerVM
import com.github.tidetune.widgets.appbar.BottomBar
import com.github.tidetune.widgets.appbar.getBottomBarSpace
import com.github.tidetune.widgets.dashboard.DashboardSubpage
import com.github.tidetune.widgets.library.LibrarySubpage
import com.github.tidetune.widgets.playlists.PlaylistsSubpage
import com.github.tidetune.widgets.settings.SettingSubpage

@Composable
fun HomePage(
    playerVM: PlayerVM = koinViewModel(),
    scaffoldPadding: PaddingValues,
) {
    val pagerState = rememberPagerState(pageCount = {
        4
    })
    val isPlaying by playerVM.playing.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        HorizontalPager(
            modifier = Modifier.padding(
                bottom = getBottomBarSpace(isPlaying, scaffoldPadding),
            ),
            state = pagerState
        ) { page ->
            if (page == 0) {
                PlaylistsSubpage()
            }
            if (page == 1) {
                LibrarySubpage()
            }
            if (page == 2) {
                DashboardSubpage()
            }
            if (page == 3) {
                SettingSubpage()
            }
        }
        BottomBar(
            bottomBarPageState = pagerState,
            scaffoldPadding = scaffoldPadding,
        )
    }
}
