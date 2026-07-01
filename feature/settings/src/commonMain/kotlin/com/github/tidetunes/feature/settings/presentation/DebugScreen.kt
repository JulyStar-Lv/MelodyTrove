package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.Res
import tidetunes.feature.settings.generated.resources.debug_trigger_kt_async_exception
import tidetunes.feature.settings.generated.resources.debug_trigger_kt_exception
import tidetunes.feature.settings.generated.resources.debug_trigger_rs_async_err
import tidetunes.feature.settings.generated.resources.debug_trigger_rs_err
import tidetunes.feature.settings.generated.resources.debug_trigger_rs_panic
import tidetunes.feature.settings.generated.resources.setting_debug

private val paddingX = 24.dp

@Composable
private fun Item(
    title: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.height(56.dp))
        Text(
            modifier = Modifier.padding(horizontal = paddingX),
            text = title,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun DebugScreen(
    onAction: (DebugAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                modifier = Modifier.padding(
                    start = paddingX, end = paddingX, top = 24.dp, bottom = 4.dp
                ),
                text = stringResource(Res.string.setting_debug),
                fontSize = 32.sp,
            )
            Box(modifier = Modifier.height(24.dp))
            Item(
                title = stringResource(Res.string.debug_trigger_rs_err),
                onClick = { onAction(DebugAction.TriggerRustError) }
            )
            Item(
                title = stringResource(Res.string.debug_trigger_rs_async_err),
                onClick = { onAction(DebugAction.TriggerRustAsyncError) }
            )
            Item(
                title = stringResource(Res.string.debug_trigger_rs_panic),
                onClick = { onAction(DebugAction.TriggerRustPanic) }
            )
            Item(
                title = stringResource(Res.string.debug_trigger_kt_exception),
                onClick = { onAction(DebugAction.TriggerKotlinError) }
            )
            Item(
                title = stringResource(Res.string.debug_trigger_kt_async_exception),
                onClick = { onAction(DebugAction.TriggerKotlinAsyncError) }
            )
        }
    }
}
