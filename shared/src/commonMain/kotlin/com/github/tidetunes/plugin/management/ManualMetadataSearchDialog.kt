package com.github.tidetunes.plugin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppTextField
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.source.api.MetaSongCandidate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ManualMetadataSearchDialog(
    track: NowPlayingTrackItem?,
    onDismiss: () -> Unit,
    service: ManualMetadataService = koinInject(),
) {
    if (track == null) return
    val scope = rememberCoroutineScope()
    var keyword by remember(track.id) {
        mutableStateOf(listOfNotNull(track.title, track.artist).joinToString(" "))
    }
    var candidates by remember(track.id) { mutableStateOf(emptyList<MetaSongCandidate>()) }
    var selected by remember(track.id) { mutableStateOf<MetaSongCandidate?>(null) }
    var message by remember(track.id) { mutableStateOf<String?>(null) }
    var searching by remember(track.id) { mutableStateOf(false) }
    var applying by remember(track.id) { mutableStateOf(false) }

    fun search() {
        if (searching || applying || keyword.isBlank()) return
        scope.launch {
            searching = true
            selected = null
            message = null
            try {
                val result = service.search(track, keyword)
                candidates = result.items
                message = when {
                    result.items.isEmpty() && result.failures.isEmpty() ->
                        "No matches. Enable at least one metadata plugin and try another keyword."
                    result.items.isEmpty() -> result.failures.first().message
                    result.failures.isNotEmpty() ->
                        "Found ${result.items.size} matches; ${result.failures.size} source(s) failed."
                    else -> null
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                candidates = emptyList()
                message = error.message ?: "Metadata search failed."
            } finally {
                searching = false
            }
        }
    }

    fun applySelected() {
        val candidate = selected ?: return
        if (searching || applying) return
        scope.launch {
            applying = true
            message = null
            try {
                val lyricFailures = service.apply(track.id, candidate)
                message = if (lyricFailures.isEmpty()) {
                    "Applied metadata for ${candidate.title}."
                } else {
                    "Applied song metadata. Lyrics were unavailable: ${lyricFailures.first().message}"
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                message = error.message ?: "Failed to apply metadata."
            } finally {
                applying = false
            }
        }
    }

    LaunchedEffect(track.id) { search() }

    TideDialog(
        show = true,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Search metadata",
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "Choose a match to update this library track. The audio file itself is not modified.",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            AppTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Keyword",
                singleLine = true,
                enabled = !searching && !applying,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TideTextButton(
                    text = if (searching) "Searching…" else "Search",
                    variant = TideTextButtonVariant.Primary,
                    size = TideTextButtonSize.Medium,
                    enabled = keyword.isNotBlank() && !searching && !applying,
                    onClick = ::search,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                candidates.forEach { candidate ->
                    MetadataCandidateRow(
                        candidate = candidate,
                        selected = candidate == selected,
                        enabled = !applying,
                        onClick = { selected = candidate },
                    )
                }
            }
            message?.let { value ->
                Text(
                    text = value,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TideTextButton(
                    text = "Close",
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Medium,
                    enabled = !applying,
                    onClick = onDismiss,
                )
                TideTextButton(
                    text = if (applying) "Applying…" else "Apply",
                    variant = TideTextButtonVariant.PrimaryFilled,
                    size = TideTextButtonSize.Medium,
                    enabled = selected != null && !searching && !applying,
                    onClick = ::applySelected,
                )
            }
        }
    }
}

@Composable
private fun MetadataCandidateRow(
    candidate: MetaSongCandidate,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(TideTunesTokens.shapes.md),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = candidate.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = listOfNotNull(candidate.artist, candidate.album).joinToString(" · ")
                .ifBlank { "Unknown artist" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        candidate.sourceId?.let { sourceId ->
            Text(
                text = sourceId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
