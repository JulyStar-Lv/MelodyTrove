package io.github.julystar.musicapp.feature.home.presentation

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
import io.github.julystar.musicapp.core.presentation.components.LocalDesignBottomContentInset
import io.github.julystar.musicapp.core.presentation.components.DesignCardSurface
import io.github.julystar.musicapp.core.presentation.components.DesignPageHeader
import io.github.julystar.musicapp.core.presentation.components.DesignStatusCard
import io.github.julystar.musicapp.core.presentation.components.DesignTabItem
import io.github.julystar.musicapp.core.presentation.components.DesignTabs
import io.github.julystar.musicapp.core.presentation.components.DesignTabsVariant
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.components.DesignTopBarBackButton
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import io.github.julystar.musicapp.feature.home.domain.ListeningDistributionBucket
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import musicapp.feature.home.generated.resources.Res
import musicapp.feature.home.generated.resources.listening_active_days
import musicapp.feature.home.generated.resources.listening_average_day
import musicapp.feature.home.generated.resources.listening_calendar
import musicapp.feature.home.generated.resources.listening_calendar_caption
import musicapp.feature.home.generated.resources.listening_favorite_album
import musicapp.feature.home.generated.resources.listening_favorite_artist
import musicapp.feature.home.generated.resources.listening_favorite_track
import musicapp.feature.home.generated.resources.listening_favorites
import musicapp.feature.home.generated.resources.listening_formats
import musicapp.feature.home.generated.resources.listening_history
import musicapp.feature.home.generated.resources.listening_history_empty
import musicapp.feature.home.generated.resources.listening_habits
import musicapp.feature.home.generated.resources.listening_late_night
import musicapp.feature.home.generated.resources.listening_morning
import musicapp.feature.home.generated.resources.listening_afternoon
import musicapp.feature.home.generated.resources.listening_evening
import musicapp.feature.home.generated.resources.listening_month_report
import musicapp.feature.home.generated.resources.listening_no_data
import musicapp.feature.home.generated.resources.listening_overview
import musicapp.feature.home.generated.resources.listening_peak_time
import musicapp.feature.home.generated.resources.listening_play_count_ranking
import musicapp.feature.home.generated.resources.listening_play_count
import musicapp.feature.home.generated.resources.listening_plays
import musicapp.feature.home.generated.resources.listening_quality
import musicapp.feature.home.generated.resources.listening_rankings
import musicapp.feature.home.generated.resources.listening_remove
import musicapp.feature.home.generated.resources.listening_streak
import musicapp.feature.home.generated.resources.listening_subtitle
import musicapp.feature.home.generated.resources.listening_time
import musicapp.feature.home.generated.resources.listening_time_ranking
import musicapp.feature.home.generated.resources.listening_title
import musicapp.feature.home.generated.resources.listening_unique_tracks
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.time.Instant

@Composable
fun ListeningScreen(
    state: ListeningState,
    onAction: (ListeningAction) -> Unit,
) {
    val bottomInset = LocalDesignBottomContentInset.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        val pagePadding = if (maxWidth < 600.dp) {
            DesignTokens.spacing.pageCompact
        } else {
            DesignTokens.spacing.pageExpanded
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = DesignTokens.adaptive.contentMaxWidth)
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
                    DesignTopBarBackButton(
                        onClick = { onAction(ListeningAction.NavigateBack) },
                        contentDescription = stringResource(Res.string.listening_title),
                    )
                    DesignPageHeader(
                        title = stringResource(Res.string.listening_title),
                        subtitle = stringResource(Res.string.listening_subtitle),
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
            item {
                DesignTabs(
                    items = listOf(
                        DesignTabItem(stringResource(Res.string.listening_overview)),
                        DesignTabItem(stringResource(Res.string.listening_calendar)),
                        DesignTabItem(stringResource(Res.string.listening_rankings)),
                    ),
                    selectedIndex = state.selectedTab.ordinal,
                    onSelectedIndexChange = { index ->
                        onAction(ListeningAction.SelectTab(ListeningTab.entries[index]))
                    },
                    variant = DesignTabsVariant.Segmented,
                )
            }
            if (state.isLoading) {
                item {
                    DesignStatusCard(
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
        DesignCardSurface {
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
        DesignCardSurface {
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
        DesignCardSurface {
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
            DesignStatusCard(
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
    DesignCardSurface {
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
    DesignCardSurface {
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
    DesignCardSurface(
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
            DesignTextButton(
                text = stringResource(Res.string.listening_remove),
                variant = DesignTextButtonVariant.Default,
                size = DesignTextButtonSize.Small,
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
    DesignCardSurface(
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
    DesignStatusCard(
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
