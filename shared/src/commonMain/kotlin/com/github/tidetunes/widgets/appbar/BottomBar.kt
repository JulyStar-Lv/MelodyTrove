package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.isRouteHome

import com.github.tidetunes.core.presentation.components.appPainterResource
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.navigation.HomeTab
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.widgets.musics.MiniPlayer
import org.koin.compose.viewmodel.koinViewModel

fun getBottomBarSpace(
    isPlaying: Boolean,
    scaffoldPadding: PaddingValues
): Dp {
    var total = 60.dp + scaffoldPadding.calculateBottomPadding()
    if (isPlaying) {
        total += 124.dp
    }
    return total
}

@Composable
fun BottomBarSpacer(
    hasCurrentMusic: Boolean,
    scaffoldPadding: PaddingValues,
) {
    Box(modifier = Modifier.height(getBottomBarSpace(hasCurrentMusic, scaffoldPadding)))
}

@Composable
fun BoxScope.BottomBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    scaffoldPadding: PaddingValues,
    playerVM: PlayerVM = koinViewModel(),
) {
    val navController = LocalNavController.current
    val _currentRoute by navController.currentBackStackEntryAsState()
    val currentRoute = if (_currentRoute?.destination?.route != null) _currentRoute!!.destination.route!! else ""

    val playbackState by playerVM.playbackState.collectAsState()

    val hasCurrentMusic = playbackState.currentItem != null

    val showBottomBar = isRouteHome(currentRoute)
    val showMiniPlayer = hasCurrentMusic && isRouteHome(currentRoute)

    if (!showBottomBar && !showMiniPlayer) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(scaffoldPadding.calculateBottomPadding())
        )
        return
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .dropShadow(
                MaterialTheme.colorScheme.surfaceVariant,
                0.dp,
                (-4).dp,
                8.dp,
            )
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
    ) {
        if (showMiniPlayer) {
            MiniPlayer()
        }
        if (showBottomBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                for (tab in HomeTab.entries) {
                    val isSelected = currentTab == tab
                    val tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    Box(modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically)
                        .clickable {
                            onTabSelected(tab)
                        }) {
                        Icon(
                            painter = appPainterResource(tab.painterRes),
                            tint = tint,
                            contentDescription = null,
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(scaffoldPadding.calculateBottomPadding())
        )
    }
}
