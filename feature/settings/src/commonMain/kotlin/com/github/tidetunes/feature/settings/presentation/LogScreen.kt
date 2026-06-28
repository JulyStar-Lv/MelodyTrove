package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.Res
import tidetunes.feature.settings.generated.resources.log_title

data class LogEntry(
    val name: String,
)

private val paddingX = 24.dp

@Composable
fun LogScreen(
    logs: List<LogEntry>,
    onAction: (LogAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                modifier = Modifier.padding(
                    start = paddingX, end = paddingX, top = 24.dp, bottom = 4.dp
                ),
                text = stringResource(Res.string.log_title),
                fontSize = 32.sp,
            )
            Text(
                modifier = Modifier.padding(horizontal = paddingX),
                text = "${logs.size} log files found",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Box(modifier = Modifier.height(24.dp))
            LazyColumn(modifier = Modifier.weight(1.0f)) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // TODO: KMP - open log file with platform-specific viewer
                            }
                    ) {
                        Text(
                            modifier = Modifier.padding(
                                horizontal = paddingX, vertical = 8.dp
                            ),
                            text = log.name,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
            Box(modifier = Modifier.height(24.dp))
        }
    }
}
