package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.appPainterResource
import com.github.tidetunes.core.presentation.components.dropShadow
import com.github.tidetunes.navigation.HomeTab
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.widgets.musics.MiniPlayer
import org.koin.compose.viewmodel.koinViewModel

fun getSidebarWidth(): Dp = 248.dp

@Composable
fun SidebarBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    playerVM: PlayerVM = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val playbackState by playerVM.playbackState.collectAsState()
    val hasCurrentMusic = playbackState.currentItem != null

    Column(
        modifier = modifier
            .width(getSidebarWidth())
            .dropShadow(
                MaterialTheme.colorScheme.surfaceVariant,
                4.dp,
                0.dp,
                8.dp,
            )
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Text(
            text = "TideTunes",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        for (tab in HomeTab.entries) {
            SidebarItem(
                tab = tab,
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (hasCurrentMusic) {
            MiniPlayer()
        }
    }
}

@Composable
private fun SidebarItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Icon(
            painter = appPainterResource(tab.painterRes),
            tint = contentColor,
            contentDescription = null,
            modifier = Modifier
                .width(22.dp)
                .height(22.dp),
        )
        Box(modifier = Modifier.width(14.dp))
        Text(
            text = tab.label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}
