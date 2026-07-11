package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import tidetunes.core.presentation.generated.resources.Res as CorePresentationRes
import tidetunes.core.presentation.generated.resources.icon_chevron_left
import tidetunes.core.presentation.generated.resources.icon_chevron_right
import tidetunes.core.presentation.generated.resources.icon_lyrics
import tidetunes.core.presentation.generated.resources.icon_mode_list
import tidetunes.core.presentation.generated.resources.icon_vertialcal_more
import tidetunes.shared.generated.resources.Res as SharedRes
import tidetunes.shared.generated.resources.icon_adjust
import tidetunes.shared.generated.resources.icon_search
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DesktopToolbar(
    onSearch: () -> Unit,
    rightPanel: DesktopRightPanel? = null,
    onRightPanelChange: (DesktopRightPanel?) -> Unit = {},
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.60f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DesktopToolButton(
                    painter = painterResource(CorePresentationRes.drawable.icon_chevron_left),
                    contentDescription = "Back",
                    filled = true,
                )
                DesktopToolButton(
                    painter = painterResource(CorePresentationRes.drawable.icon_chevron_right),
                    contentDescription = "Forward",
                    filled = true,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 384.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.sm))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(SharedRes.drawable.icon_search),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Search TideTunes…",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DesktopToolButton(
                    painter = painterResource(CorePresentationRes.drawable.icon_lyrics),
                    contentDescription = "Lyrics",
                    active = rightPanel == DesktopRightPanel.Lyrics,
                    onClick = {
                        onRightPanelChange(
                            if (rightPanel == DesktopRightPanel.Lyrics) null else DesktopRightPanel.Lyrics,
                        )
                    },
                )
                DesktopToolButton(
                    painter = painterResource(CorePresentationRes.drawable.icon_mode_list),
                    contentDescription = "Queue",
                    active = rightPanel == DesktopRightPanel.Queue,
                    onClick = {
                        onRightPanelChange(
                            if (rightPanel == DesktopRightPanel.Queue) null else DesktopRightPanel.Queue,
                        )
                    },
                )
                DesktopToolButton(
                    painter = painterResource(SharedRes.drawable.icon_adjust),
                    contentDescription = "Theme",
                    onClick = onToggleTheme,
                )
                DesktopToolButton(
                    painter = painterResource(CorePresentationRes.drawable.icon_vertialcal_more),
                    contentDescription = "Notifications",
                    showDot = true,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun DesktopToolButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit = {},
    filled: Boolean = false,
    showDot: Boolean = false,
    active: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(if (filled) 28.dp else 32.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.xs))
            .background(
                if (active) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else if (filled) {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            tint = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            contentDescription = contentDescription,
            modifier = Modifier.size(if (filled) 14.dp else 16.dp),
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp)
                    .size(6.dp)
                    .background(
                        MiuixTheme.colorScheme.primary,
                        RoundedCornerShape(TideTunesTokens.shapes.full),
                    ),
            )
        }
    }
}

enum class DesktopRightPanel { Lyrics, Queue }

@Composable
fun DesktopRightPanelContent(
    panel: DesktopRightPanel,
    hasCurrentMusic: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(288.dp)
            .fillMaxHeight()
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.60f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (panel == DesktopRightPanel.Lyrics) "Lyrics" else "Queue",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "×", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MiuixTheme.colorScheme.outline))
        if (!hasCurrentMusic) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Nothing playing", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        } else if (panel == DesktopRightPanel.Lyrics) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                desktopLyrics.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = MiuixTheme.textStyles.body2,
                        color = if (index == 3) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "Up Next · ${desktopQueue.size} songs", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                desktopQueue.forEachIndexed { index, song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (index == 0) MiuixTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(song.first, style = MiuixTheme.textStyles.body2, color = if (index == 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface)
                            Text(song.second, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        Text(song.third, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }
    }
}

private val desktopLyrics = listOf(
    "City lights dissolve into the rain",
    "Signals drift across the avenue",
    "The frequency shifts as the night moves on",
    "Midnight cascade, carry me away",
    "Every echo finds another wave",
    "Midnight cascade, where frequencies are found",
)

private val desktopQueue = listOf(
    Triple("Midnight Cascade", "Luna Waves", "4:12"),
    Triple("Neon Undertow", "Vector Bloom", "3:48"),
    Triple("Silver Tide", "Coastal Drift", "3:55"),
    Triple("Aurora Sequence", "Polar Echo", "5:02"),
)
