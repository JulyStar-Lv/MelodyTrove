package io.github.julystar.musicapp.plugin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.components.AppTextField
import io.github.julystar.musicapp.core.presentation.components.DesignDialog
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
        mutableStateOf(listOfNotNull(activeTrack.title, activeTrack.artist).joinToString(" "))
    }
    var candidates by remember(activeTrack.id) { mutableStateOf(emptyList<MetaSongCandidate>()) }
    var selected by remember(activeTrack.id) { mutableStateOf<MetaSongCandidate?>(null) }
    var message by remember(activeTrack.id) { mutableStateOf<String?>(null) }
    var searching by remember(activeTrack.id) { mutableStateOf(false) }
    var applying by remember(activeTrack.id) { mutableStateOf(false) }
    var resetting by remember(activeTrack.id) { mutableStateOf(false) }

    fun search() {
        if (searching || applying || resetting || keyword.isBlank()) return
        scope.launch {
            searching = true
            selected = null
            message = null
            try {
                val result = service.search(activeTrack, keyword)
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
        if (searching || applying || resetting) return
        scope.launch {
            applying = true
            message = null
            try {
                val lyricFailures = service.apply(activeTrack.id, candidate)
                if (lyricFailures.isEmpty()) {
                    onDismiss()
                } else {
                    message = metadataApplyMessage(candidate.title, lyricFailures)
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

    fun resetFromFile() {
        if (searching || applying || resetting) return
        scope.launch {
            resetting = true
            message = null
            try {
                service.resetFromFile(activeTrack.id)
                onDismiss()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                message = error.message ?: "Failed to reset metadata from the music file."
            } finally {
                resetting = false
            }
        }
    }

    LaunchedEffect(activeTrack.id, dialogVisible) {
        if (dialogVisible) {
            keyword = listOfNotNull(activeTrack.title, activeTrack.artist).joinToString(" ")
            candidates = emptyList()
            selected = null
            message = null
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
                text = "Search metadata",
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "Choose a match to update this library track, or reset from the current " +
                    "file tags. The audio file itself is not modified.",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            AppTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Keyword",
                singleLine = true,
                enabled = !searching && !applying && !resetting,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                DesignTextButton(
                    text = if (searching) "Searching…" else "Search",
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
                candidates.forEach { candidate ->
                    MetadataCandidateRow(
                        candidate = candidate,
                        selected = candidate == selected,
                        enabled = !applying && !resetting,
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
                DesignTextButton(
                    text = if (resetting) "Resetting…" else "Reset from file",
                    variant = DesignTextButtonVariant.Default,
                    size = DesignTextButtonSize.Medium,
                    enabled = !searching && !applying && !resetting,
                    onClick = ::resetFromFile,
                )
                DesignTextButton(
                    text = if (applying) "Applying…" else "Apply",
                    variant = DesignTextButtonVariant.PrimaryFilled,
                    size = DesignTextButtonSize.Medium,
                    enabled = selected != null && !searching && !applying && !resetting,
                    onClick = ::applySelected,
                )
            }
        }
    }
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
