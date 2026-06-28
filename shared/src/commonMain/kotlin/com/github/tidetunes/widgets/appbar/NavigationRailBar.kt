package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.appPainterResource
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.navigation.HomeTab
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.widgets.musics.MiniPlayer
import org.koin.compose.viewmodel.koinViewModel

fun getNavigationRailWidth(): Dp = 80.dp

@Composable
fun NavigationRailBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    playerVM: PlayerVM = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val playbackState by playerVM.playbackState.collectAsState()
    val hasCurrentMusic = playbackState.currentItem != null

    Column(
        modifier = modifier
            .width(getNavigationRailWidth())
            .dropShadow(
                MaterialTheme.colorScheme.surfaceVariant,
                4.dp,
                0.dp,
                8.dp,
            )
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (hasCurrentMusic) {
            MiniPlayer()
        }
        NavigationRail(
            modifier = Modifier.weight(1f),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            for (tab in HomeTab.entries) {
                NavigationRailItem(
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            painter = appPainterResource(tab.painterRes),
                            contentDescription = null,
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp),
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                        )
                    },
                )
            }
        }
    }
}
