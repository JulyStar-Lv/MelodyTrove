package com.github.tidetunes.widgets.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.navigation.HomeTab
import org.jetbrains.compose.resources.painterResource
import tidetunes.shared.generated.resources.Res
import tidetunes.shared.generated.resources.icon_adjust
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun getSidebarWidth(windowSizeClass: WindowSizeClass = WindowSizeClass.Large): Dp = 224.dp

@Composable
fun SidebarBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = WindowSizeClass.Large,
) {
    val shapes = TideTunesTokens.shapes
    val sidebarColor = if (MiuixTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF110E1E)
    } else {
        Color(0xFFEDEAF7)
    }

    Box(
        modifier = modifier
            .width(getSidebarWidth(windowSizeClass))
            .background(sidebarColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(TideTunesBrand.Primary, TideTunesBrand.Secondary),
                            ),
                            shape = RoundedCornerShape(shapes.sm),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(HomeTab.LIBRARY.painterRes),
                        tint = Color.White,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TideTunes",
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "One Library. Every Source.",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote2,
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "DISCOVER",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Column(modifier = Modifier.selectableGroup()) {
                for (tab in HomeTab.entries) {
                    SidebarItem(
                        tab = tab,
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TideTunesTokens.adaptive.minimumTouchTarget)
                    .clip(RoundedCornerShape(shapes.compactCard))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_adjust),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isDark) "Light Mode" else "Dark Mode",
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.64f)),
        )
    }
}

@Composable
private fun SidebarItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes
    val itemShape = RoundedCornerShape(shapes.compactCard)
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    }
    val backgroundBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                TideTunesBrand.Primary.copy(alpha = 0.16f),
                TideTunesBrand.Secondary.copy(alpha = 0.13f),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(TideTunesTokens.adaptive.minimumTouchTarget)
            .clip(itemShape)
            .background(backgroundBrush)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = tab.label
                this.role = Role.Tab
                this.selected = selected
                onClick { onClick(); true }
            }
            .padding(horizontal = 12.dp),
    ) {
        Icon(
            painter = painterResource(tab.painterRes),
            tint = contentColor,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = tab.label,
            color = contentColor,
            style = MiuixTheme.textStyles.body1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(shapes.full)),
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
