package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.LocalTideBottomContentInset
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideStatusCard
import com.github.tidetunes.core.presentation.components.TideTabItem
import com.github.tidetunes.core.presentation.components.TideTabs
import com.github.tidetunes.core.presentation.components.TideTabsVariant
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.components.TideTopBarBackButton
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.feature.home.domain.ListeningDistributionBucket
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.home.generated.resources.Res
import tidetunes.feature.home.generated.resources.listening_active_days
import tidetunes.feature.home.generated.resources.listening_average_day
import tidetunes.feature.home.generated.resources.listening_calendar
import tidetunes.feature.home.generated.resources.listening_calendar_caption
import tidetunes.feature.home.generated.resources.listening_favorite_album
import tidetunes.feature.home.generated.resources.listening_favorite_artist
import tidetunes.feature.home.generated.resources.listening_favorite_track
import tidetunes.feature.home.generated.resources.listening_favorites
import tidetunes.feature.home.generated.resources.listening_formats
import tidetunes.feature.home.generated.resources.listening_history
import tidetunes.feature.home.generated.resources.listening_history_empty
import tidetunes.feature.home.generated.resources.listening_habits
import tidetunes.feature.home.generated.resources.listening_late_night
import tidetunes.feature.home.generated.resources.listening_morning
import tidetunes.feature.home.generated.resources.listening_afternoon
import tidetunes.feature.home.generated.resources.listening_evening
import tidetunes.feature.home.generated.resources.listening_month_report
import tidetunes.feature.home.generated.resources.listening_no_data
import tidetunes.feature.home.generated.resources.listening_overview
import tidetunes.feature.home.generated.resources.listening_peak_time
import tidetunes.feature.home.generated.resources.listening_play_count_ranking
import tidetunes.feature.home.generated.resources.listening_play_count
import tidetunes.feature.home.generated.resources.listening_plays
import tidetunes.feature.home.generated.resources.listening_quality
import tidetunes.feature.home.generated.resources.listening_rankings
import tidetunes.feature.home.generated.resources.listening_remove
import tidetunes.feature.home.generated.resources.listening_streak
import tidetunes.feature.home.generated.resources.listening_subtitle
import tidetunes.feature.home.generated.resources.listening_time
import tidetunes.feature.home.generated.resources.listening_time_ranking
import tidetunes.feature.home.generated.resources.listening_title
import tidetunes.feature.home.generated.resources.listening_unique_tracks
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.time.Instant

@Composable
fun ListeningScreen(
    state: ListeningState,
    onAction: (ListeningAction) -> Unit,
) {
    val bottomInset = LocalTideBottomContentInset.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        val pagePadding = if (maxWidth < 600.dp) {
            TideTunesTokens.spacing.pageCompact
        } else {
            TideTunesTokens.spacing.pageExpanded
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TideTunesTokens.adaptive.contentMaxWidth)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = pagePadding,
                end = pagePadding,
                bottom = bottomInset + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TideTopBarBackButton(
                        onClick = { onAction(ListeningAction.NavigateBack) },
                        contentDescription = stringResource(Res.string.listening_title),
                    )
                    TidePageHeader(
                        title = stringResource(Res.string.listening_title),
                        subtitle = stringResource(Res.string.listening_subtitle),
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
            item {
                TideTabs(
                    items = listOf(
                        TideTabItem(stringResource(Res.string.listening_overview)),
                        TideTabItem(stringResource(Res.string.listening_calendar)),
                        TideTabItem(stringResource(Res.string.listening_rankings)),
                    ),
                    selectedIndex = state.selectedTab.ordinal,
                    onSelectedIndexChange = { index ->
                        onAction(ListeningAction.SelectTab(ListeningTab.entries[index]))
                    },
                    variant = TideTabsVariant.Segmented,
                )
            }
            if (state.isLoading) {
                item {
                    TideStatusCard(
                        title = stringResource(Res.string.listening_title),
                        message = stringResource(Res.string.listening_no_data),
                        loading = true,
                    )
                }
            } else {
                when (state.selectedTab) {
                    ListeningTab.Overview -> overviewItems(state)
                    ListeningTab.Calendar -> calendarItems(state, onAction)
                    ListeningTab.Rankings -> rankingItems(state, onAction)
                }
            }
        }
    }
}

private fun LazyListScope.overviewItems(state: ListeningState) {
    item {
        SectionTitle(
            title = stringResource(Res.string.listening_month_report),
            subtitle = state.monthLabel,
        )
    }
    item {
        TideCardSurface {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MetricRow(
                    firstLabel = stringResource(Res.string.listening_play_count),
                    firstValue = state.monthPlayCount.toString(),
                    secondLabel = stringResource(Res.string.listening_unique_tracks),
                    secondValue = state.monthUniqueTrackCount.toString(),
                )
                MetricRow(
                    firstLabel = stringResource(Res.string.listening_time),
                    firstValue = formatListeningDuration(state.monthListenedMs),
                    secondLabel = stringResource(Res.string.listening_active_days),
                    secondValue = state.activeDays.toString(),
                )
            }
        }
    }
    item { SectionTitle(stringResource(Res.string.listening_habits)) }
    item {
        TideCardSurface {
            Column {
                MetricRow(
                    firstLabel = stringResource(Res.string.listening_streak),
                    firstValue = state.longestStreakDays.toString(),
                    secondLabel = stringResource(Res.string.listening_average_day),
                    secondValue = formatListeningDuration(state.averagePerActiveDayMs),
                )
                Spacer(Modifier.height(14.dp))
                LabeledValue(
                    label = stringResource(Res.string.listening_peak_time),
                    value = state.peakTimePeriod?.localizedLabel()
                        ?: stringResource(Res.string.listening_no_data),
                )
            }
        }
    }
    item { SectionTitle(stringResource(Res.string.listening_favorites)) }
    item {
        TideCardSurface {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                InsightRow(
                    stringResource(Res.string.listening_favorite_track),
                    state.favoriteTrack,
                )
                InsightRow(
                    stringResource(Res.string.listening_favorite_artist),
                    state.favoriteArtist,
                )
                InsightRow(
                    stringResource(Res.string.listening_favorite_album),
                    state.favoriteAlbum,
                )
            }
        }
    }
    item {
        DistributionCard(
            title = stringResource(Res.string.listening_formats),
            buckets = state.formatDistribution,
        )
    }
    item {
        DistributionCard(
            title = stringResource(Res.string.listening_quality),
            buckets = state.qualityDistribution,
        )
    }
}

private fun LazyListScope.calendarItems(
    state: ListeningState,
    onAction: (ListeningAction) -> Unit,
) {
    item {
        SectionTitle(
            title = stringResource(Res.string.listening_calendar),
            subtitle = stringResource(Res.string.listening_calendar_caption),
        )
    }
    item {
        ListeningHeatmap(days = state.calendarDays)
    }
    item { SectionTitle(stringResource(Res.string.listening_history)) }
    if (state.recentHistory.isEmpty()) {
        item {
            TideStatusCard(
                title = stringResource(Res.string.listening_history),
                message = stringResource(Res.string.listening_history_empty),
            )
        }
    } else {
        items(state.recentHistory, key = ListeningHistoryItem::id) { item ->
            HistoryRow(
                item = item,
                onPlay = { onAction(ListeningAction.PlayTrack(item.trackId)) },
                onRemove = { onAction(ListeningAction.RemoveHistoryEntry(item.id)) },
            )
        }
    }
}

private fun LazyListScope.rankingItems(
    state: ListeningState,
    onAction: (ListeningAction) -> Unit,
) {
    item { SectionTitle(stringResource(Res.string.listening_time_ranking)) }
    if (state.durationRanking.isEmpty()) {
        item { EmptyRankingCard() }
    } else {
        items(state.durationRanking) { track ->
            RankingRow(
                rank = state.durationRanking.indexOf(track) + 1,
                track = track,
                value = formatListeningDuration(track.listenedMs),
                onPlay = { onAction(ListeningAction.PlayTrack(track.trackId)) },
            )
        }
    }
    item { SectionTitle(stringResource(Res.string.listening_play_count_ranking)) }
    if (state.playCountRanking.isEmpty()) {
        item { EmptyRankingCard() }
    } else {
        items(state.playCountRanking) { track ->
            RankingRow(
                rank = state.playCountRanking.indexOf(track) + 1,
                track = track,
                value = stringResource(Res.string.listening_plays, track.playCount),
                onPlay = { onAction(ListeningAction.PlayTrack(track.trackId)) },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}

@Composable
private fun MetricRow(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Metric(firstLabel, firstValue, Modifier.weight(1f))
        Metric(secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(value, color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InsightRow(label: String, insight: ListeningInsight?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = insight?.title ?: stringResource(Res.string.listening_no_data),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        insight?.let {
            Text(
                text = stringResource(Res.string.listening_plays, it.playCount),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DistributionCard(
    title: String,
    buckets: List<ListeningDistributionBucket>,
) {
    val total = buckets.sumOf(ListeningDistributionBucket::trackCount).coerceAtLeast(1)
    TideCardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            if (buckets.isEmpty()) {
                Text(
                    stringResource(Res.string.listening_no_data),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                buckets.take(6).forEach { bucket ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(bucket.label, color = MiuixTheme.colorScheme.onSurface)
                            Text(
                                bucket.trackCount.toString(),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(bucket.trackCount.toFloat() / total)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MiuixTheme.colorScheme.primary),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningHeatmap(days: List<ListeningDay>) {
    val maxListenedMs = days.maxOfOrNull(ListeningDay::listenedMs)?.coerceAtLeast(1L) ?: 1L
    TideCardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    week.forEach { day ->
                        val intensity = day.listenedMs.toFloat() / maxListenedMs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (day.listenedMs == 0L) {
                                        MiuixTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        MiuixTheme.colorScheme.primary.copy(
                                            alpha = 0.20f + intensity * 0.80f,
                                        )
                                    },
                                ),
                        )
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: ListeningHistoryItem,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    TideCardSurface(
        modifier = Modifier.heightIn(min = 64.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(item.artist, formatListeningTimestamp(item.playedAtEpochMs))
                        .joinToString(" · "),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatListeningDuration(item.listenedMs),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
            TideTextButton(
                text = stringResource(Res.string.listening_remove),
                variant = TideTextButtonVariant.Default,
                size = TideTextButtonSize.Small,
                onClick = onRemove,
            )
        }
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    track: ListeningRankedTrack,
    value: String,
    onPlay: () -> Unit,
) {
    TideCardSurface(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = rank.toString().padStart(2, '0'),
                modifier = Modifier.width(26.dp),
                color = if (rank <= 3) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
                fontWeight = FontWeight.Bold,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let {
                    Text(
                        it,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                value,
                color = MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyRankingCard() {
    TideStatusCard(
        title = stringResource(Res.string.listening_rankings),
        message = stringResource(Res.string.listening_no_data),
    )
}

@Composable
private fun ListeningTimePeriod.localizedLabel(): String = stringResource(
    when (this) {
        ListeningTimePeriod.LateNight -> Res.string.listening_late_night
        ListeningTimePeriod.Morning -> Res.string.listening_morning
        ListeningTimePeriod.Afternoon -> Res.string.listening_afternoon
        ListeningTimePeriod.Evening -> Res.string.listening_evening
    },
)

private fun formatListeningDuration(totalMs: Long): String {
    val totalMinutes = (totalMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatListeningTimestamp(epochMs: Long): String {
    val value = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(value.date)
        append(' ')
        append(value.hour.toString().padStart(2, '0'))
        append(':')
        append(value.minute.toString().padStart(2, '0'))
    }
}
