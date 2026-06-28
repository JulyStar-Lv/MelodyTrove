package com.github.tidetunes.core.presentation.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class TideTunesContextMenuItem(
    val label: StringResource,
    val onClick: () -> Unit,
    val isError: Boolean = false
) {
}

@Composable
fun TideTunesContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<TideTunesContextMenuItem>
) {
    val scope = rememberCoroutineScope()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        for (item in items) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(item.label),
                        color = if (!item.isError) { Color.Unspecified } else { MaterialTheme.colorScheme.error }
                    )
                },
                onClick = {
                    scope.launch {
                        delay(160)
                        onDismissRequest()
                    }
                    item.onClick()
                }
            )
        }
    }
}
