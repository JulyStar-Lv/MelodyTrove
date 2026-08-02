package io.github.julystar.musicapp.plugin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.components.AppTextField
import io.github.julystar.musicapp.core.presentation.components.DesignDialog
import io.github.julystar.musicapp.core.presentation.components.DesignLoadingIndicator
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import io.github.julystar.musicapp.service.playback.presentation.nowplaying.NowPlayingTrackItem
import io.github.julystar.musicapp.source.api.MetaSongCandidate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import musicapp.shared.generated.resources.Res
import musicapp.shared.generated.resources.manual_metadata_applied_without_lyrics
import musicapp.shared.generated.resources.manual_metadata_apply
import musicapp.shared.generated.resources.manual_metadata_apply_failed
import musicapp.shared.generated.resources.manual_metadata_applying
import musicapp.shared.generated.resources.manual_metadata_keyword
import musicapp.shared.generated.resources.manual_metadata_no_matches
import musicapp.shared.generated.resources.manual_metadata_no_sources
import musicapp.shared.generated.resources.manual_metadata_partial_failure
import musicapp.shared.generated.resources.manual_metadata_reset
import musicapp.shared.generated.resources.manual_metadata_reset_failed
import musicapp.shared.generated.resources.manual_metadata_resetting
import musicapp.shared.generated.resources.manual_metadata_results
import musicapp.shared.generated.resources.manual_metadata_search
import musicapp.shared.generated.resources.manual_metadata_search_failed
import musicapp.shared.generated.resources.manual_metadata_searching
import musicapp.shared.generated.resources.manual_metadata_source
import musicapp.shared.generated.resources.manual_metadata_summary
import musicapp.shared.generated.resources.manual_metadata_title
import musicapp.shared.generated.resources.manual_metadata_unknown_artist

private sealed interface ManualMetadataFeedback {
    data class SearchCompleted(
        val resultCount: Int,
        val failedSourceCount: Int,
        val queriedSourceCount: Int,
    ) : ManualMetadataFeedback

    data class AppliedWithoutLyrics(val title: String) : ManualMetadataFeedback
    data object SearchFailed : ManualMetadataFeedback
    data object ApplyFailed : ManualMetadataFeedback
    data object ResetFailed : ManualMetadataFeedback
}

@Composable
fun ManualMetadataSearchDialog(
    track: NowPlayingTrackItem?,
    onDismiss: () -> Unit,
    service: ManualMetadataService = koinInject(),
) {
    val dialogVisible = track != null
    var retainedTrack by remember { mutableStateOf(track) }
    SideEffect {
        if (track != null) retainedTrack = track
    }
    val activeTrack = track ?: retainedTrack ?: return
    val scope = rememberCoroutineScope()
    var keyword by remember(activeTrack.id) {
        mutableStateOf(defaultManualMetadataKeyword(activeTrack))
    }
    var candidates by remember(activeTrack.id) { mutableStateOf(emptyList<MetaSongCandidate>()) }
    var selected by remember(activeTrack.id) { mutableStateOf<MetaSongCandidate?>(null) }
    var feedback by remember(activeTrack.id) { mutableStateOf<ManualMetadataFeedback?>(null) }
    var searching by remember(activeTrack.id) { mutableStateOf(false) }
    var applying by remember(activeTrack.id) { mutableStateOf(false) }
    var resetting by remember(activeTrack.id) { mutableStateOf(false) }

    fun search() {
        if (searching || applying || resetting || keyword.isBlank()) return
        scope.launch {
            searching = true
            candidates = emptyList()
            selected = null
            feedback = null
            try {
                val result = service.search(activeTrack, keyword)
                candidates = result.items
                selected = result.items.firstOrNull()
                feedback = ManualMetadataFeedback.SearchCompleted(
                    resultCount = result.items.size,
                    failedSourceCount = result.failures.size,
                    queriedSourceCount = result.queriedSourceCount,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                candidates = emptyList()
                feedback = ManualMetadataFeedback.SearchFailed
            } finally {
                searching = false
            }
        }
    }

    fun applySelected() {
        val candidate = selected ?: return
        if (searching || applying || resetting) return
        scope.launch {
            applying = true
            feedback = null
            try {
                val lyricFailures = service.apply(activeTrack.id, candidate)
                if (lyricFailures.isEmpty()) {
                    onDismiss()
                } else {
                    feedback = ManualMetadataFeedback.AppliedWithoutLyrics(candidate.title)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                feedback = ManualMetadataFeedback.ApplyFailed
            } finally {
                applying = false
            }
        }
    }

    fun resetFromFile() {
        if (searching || applying || resetting) return
        scope.launch {
            resetting = true
            feedback = null
            try {
                service.resetFromFile(activeTrack.id)
                onDismiss()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                feedback = ManualMetadataFeedback.ResetFailed
            } finally {
                resetting = false
            }
        }
    }

    LaunchedEffect(activeTrack.id, dialogVisible) {
        if (dialogVisible) {
            keyword = defaultManualMetadataKeyword(activeTrack)
            candidates = emptyList()
            selected = null
            feedback = null
            searching = false
            applying = false
            resetting = false
            search()
        } else {
            scope.coroutineContext.cancelChildren()
            searching = false
            applying = false
            resetting = false
        }
    }

    DesignDialog(
        show = dialogVisible,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.manual_metadata_title),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.manual_metadata_summary),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(DesignTokens.shapes.md),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = activeTrack.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                activeTrack.artist?.takeIf(String::isNotBlank)?.let { artist ->
                    Text(
                        text = artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            AppTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(Res.string.manual_metadata_keyword),
                singleLine = true,
                enabled = !searching && !applying && !resetting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                DesignTextButton(
                    text = if (searching) {
                        stringResource(Res.string.manual_metadata_searching)
                    } else {
                        stringResource(Res.string.manual_metadata_search)
                    },
                    variant = DesignTextButtonVariant.Primary,
                    size = DesignTextButtonSize.Medium,
                    enabled = keyword.isNotBlank() && !searching && !applying && !resetting,
                    onClick = ::search,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (searching) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesignLoadingIndicator(size = 24.dp, strokeWidth = 2.dp)
                    }
                }
                candidates.forEach { candidate ->
                    MetadataCandidateRow(
                        candidate = candidate,
                        selected = candidate == selected,
                        enabled = !applying && !resetting,
                        onClick = { selected = candidate },
                    )
                }
            }
            feedback?.let { value ->
                Text(
                    text = manualMetadataFeedbackText(value),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                DesignTextButton(
                    text = if (resetting) {
                        stringResource(Res.string.manual_metadata_resetting)
                    } else {
                        stringResource(Res.string.manual_metadata_reset)
                    },
                    variant = DesignTextButtonVariant.Default,
                    size = DesignTextButtonSize.Medium,
                    enabled = !searching && !applying && !resetting,
                    onClick = ::resetFromFile,
                )
                DesignTextButton(
                    text = if (applying) {
                        stringResource(Res.string.manual_metadata_applying)
                    } else {
                        stringResource(Res.string.manual_metadata_apply)
                    },
                    variant = DesignTextButtonVariant.PrimaryFilled,
                    size = DesignTextButtonSize.Medium,
                    enabled = selected != null && !searching && !applying && !resetting,
                    onClick = ::applySelected,
                )
            }
        }
    }
}

private fun defaultManualMetadataKeyword(track: NowPlayingTrackItem): String =
    listOfNotNull(
        track.title.trim().takeIf(String::isNotEmpty),
        track.artist?.trim()?.takeIf(String::isNotEmpty),
    ).joinToString(" ")

@Composable
private fun manualMetadataFeedbackText(feedback: ManualMetadataFeedback): String = when (feedback) {
    is ManualMetadataFeedback.SearchCompleted -> when {
        feedback.queriedSourceCount == 0 ->
            stringResource(Res.string.manual_metadata_no_sources)
        feedback.resultCount == 0 && feedback.failedSourceCount > 0 ->
            stringResource(Res.string.manual_metadata_search_failed)
        feedback.resultCount == 0 ->
            stringResource(Res.string.manual_metadata_no_matches)
        feedback.failedSourceCount > 0 ->
            stringResource(
                Res.string.manual_metadata_partial_failure,
                feedback.resultCount,
                feedback.failedSourceCount,
            )
        else -> stringResource(Res.string.manual_metadata_results, feedback.resultCount)
    }
    is ManualMetadataFeedback.AppliedWithoutLyrics -> stringResource(
        Res.string.manual_metadata_applied_without_lyrics,
        feedback.title,
    )
    ManualMetadataFeedback.SearchFailed ->
        stringResource(Res.string.manual_metadata_search_failed)
    ManualMetadataFeedback.ApplyFailed ->
        stringResource(Res.string.manual_metadata_apply_failed)
    ManualMetadataFeedback.ResetFailed ->
        stringResource(Res.string.manual_metadata_reset_failed)
}

internal fun metadataApplyMessage(
    title: String,
    lyricFailures: List<MetadataLookupFailure>,
): String = if (lyricFailures.isEmpty()) {
    "Applied metadata for $title."
} else {
    "Applied metadata for $title. Lyrics were unavailable from the selected source."
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
                shape = RoundedCornerShape(DesignTokens.shapes.md),
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
                .ifBlank { stringResource(Res.string.manual_metadata_unknown_artist) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        candidate.sourceId?.let { sourceId ->
            Text(
                text = buildList {
                    candidate.date?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
                    candidate.durationMs?.let { durationMs -> add(formatMetadataDuration(durationMs)) }
                    add(stringResource(Res.string.manual_metadata_source, sourceId))
                }.joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

private fun formatMetadataDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
