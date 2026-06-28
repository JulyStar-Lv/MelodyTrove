package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.Res
import tidetunes.feature.settings.generated.resources.icon_github
import tidetunes.feature.settings.generated.resources.icon_info
import tidetunes.feature.settings.generated.resources.icon_log
import tidetunes.feature.settings.generated.resources.icon_vertialcal_more
import tidetunes.feature.settings.generated.resources.setting_about
import tidetunes.feature.settings.generated.resources.setting_debug
import tidetunes.feature.settings.generated.resources.setting_git_repo
import tidetunes.feature.settings.generated.resources.setting_log
import tidetunes.feature.settings.generated.resources.setting_more
import tidetunes.feature.settings.generated.resources.setting_version

private val paddingX = 24.dp

@Composable
private fun Title(title: String) {
    Column {
        Text(
            text = title,
            letterSpacing = 1.sp,
            fontSize = 14.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun Item(
    iconPainter: Painter,
    title: String,
    content: String?,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.height(56.dp))
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Box(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
            )
            if (content != null) {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    appVersion: String = "",
    onAction: (SettingsAction) -> Unit,
) {
    val gitUrl = "https://github.com/hpp2334/ease-music-player"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingX, paddingX)
            .verticalScroll(rememberScrollState())
    ) {
        Title(title = stringResource(Res.string.setting_debug))
        Item(
            iconPainter = painterResource(Res.drawable.icon_log),
            title = stringResource(Res.string.setting_log),
            content = null,
            onClick = { onAction(SettingsAction.NavigateToLog) }
        )
        Item(
            iconPainter = painterResource(Res.drawable.icon_vertialcal_more),
            title = stringResource(Res.string.setting_more),
            content = null,
            onClick = { onAction(SettingsAction.NavigateToDebugMore) }
        )
        Title(title = stringResource(Res.string.setting_about))
        Item(
            iconPainter = painterResource(Res.drawable.icon_github),
            title = stringResource(Res.string.setting_git_repo),
            content = gitUrl,
            onClick = { onAction(SettingsAction.OpenGitRepo(gitUrl)) }
        )
        Item(
            iconPainter = painterResource(Res.drawable.icon_info),
            title = stringResource(Res.string.setting_version),
            content = appVersion,
            onClick = {}
        )
    }
}
