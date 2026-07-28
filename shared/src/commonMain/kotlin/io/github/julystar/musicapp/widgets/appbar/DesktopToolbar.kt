package io.github.julystar.musicapp.widgets.appbar

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
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import org.jetbrains.compose.resources.painterResource
import musicapp.core.presentation.generated.resources.Res as CorePresentationRes
import musicapp.core.presentation.generated.resources.icon_chevron_left
import musicapp.core.presentation.generated.resources.icon_chevron_right
import musicapp.core.presentation.generated.resources.icon_lyrics
import musicapp.core.presentation.generated.resources.icon_vertialcal_more
import musicapp.shared.generated.resources.Res as SharedRes
import musicapp.shared.generated.resources.icon_adjust
import musicapp.shared.generated.resources.icon_search
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
                    .clip(RoundedCornerShape(DesignTokens.shapes.sm))
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
                    text = "Search MelodyTrove…",
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
            .clip(RoundedCornerShape(DesignTokens.shapes.xs))
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
                        RoundedCornerShape(DesignTokens.shapes.full),
                    ),
            )
        }
    }
}

enum class DesktopRightPanel { Lyrics }

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
                text = "Lyrics",
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
        } else {
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
