package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesFontFamilies
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// --- MediaSkeleton ---
// Shimmer placeholder for loading music content rows.

@Composable
fun MediaSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(6) {
            MediaSkeletonRow()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun MediaSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(
            width = 48.dp,
            height = 48.dp,
            radius = 6.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(
                height = 14.dp,
                widthFraction = 0.7f,
                radius = 4.dp,
            )
            Spacer(Modifier.height(6.dp))
            SkeletonBlock(
                height = 12.dp,
                widthFraction = 0.5f,
                radius = 4.dp,
            )
        }
        Spacer(Modifier.width(8.dp))
        SkeletonBlock(
            width = 40.dp,
            height = 12.dp,
            radius = 4.dp,
        )
    }
}

@Composable
fun TideTrackNumberBadge(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)
    val backgroundBrush = if (active) {
        Brush.linearGradient(
            listOf(
                TideTunesBrand.Primary,
                TideTunesBrand.Secondary,
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                MiuixTheme.colorScheme.secondaryContainer,
                MiuixTheme.colorScheme.secondaryContainer,
            ),
        )
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.ifBlank { "--" },
            color = if (active) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSecondaryContainer
            },
            style = MiuixTheme.textStyles.footnote1.copy(fontFamily = TideTunesFontFamilies.Mono),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TideMusicArtworkTile(
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.md)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.45f),
                    ),
                ),
            ),
    )
}

@Composable
fun TideTrackListRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    duration: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    TideCardSurface(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            duration?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                )
            }
            trailing?.invoke()
        }
    }
}

// --- TrackRow (shared) ---
// Generic reusable track row for Library, Search, Playlist detail, and Album views.

@Composable
fun AppTrackRow(
    title: String,
    artist: String?,
    duration: String?,
    modifier: Modifier = Modifier,
    cover: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)

    val content = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cover?.let {
                it()
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist != null) {
                    Text(
                        text = artist,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            duration?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = it,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            trailing?.let {
                Spacer(Modifier.width(4.dp))
                it()
            }
        }
    }

    if (onClick != null) {
        Box(modifier = rowModifier.clickable(onClick = onClick)) {
            content()
        }
    } else {
        Box(modifier = rowModifier) {
            content()
        }
    }
}
