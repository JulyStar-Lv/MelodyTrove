package com.github.tidetune.widgets.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetune.singleton.LibraryTrackItem
import com.github.tidetune.viewmodels.LibraryVM
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.library_empty
import tidetune.shared.generated.resources.library_title
import tidetune.shared.generated.resources.music_count_unit

@Composable
fun LibrarySubpage(
    libraryVM: LibraryVM = koinViewModel(),
) {
    val tracks by libraryVM.tracks.collectAsState()

    if (tracks.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = stringResource(Res.string.library_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(Res.string.library_title),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
            )
            Text(
                text = "${tracks.size} ${stringResource(Res.string.music_count_unit)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(tracks, key = { it.id }) { track ->
                LibraryTrackRow(track)
            }
        }
    }
}

@Composable
private fun LibraryTrackRow(track: LibraryTrackItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = track.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist ?: "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = track.durationMs.durationText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

private fun Long?.durationText(): String {
    val duration = this?.milliseconds ?: return "--:--"
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
