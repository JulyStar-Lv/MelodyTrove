package com.github.tidetunes.feature.importing.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TideTunesCheckbox
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.core.presentation.platform.TideTunesBackHandler
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeType
import tidetunes.feature.importing.generated.resources.Res
import tidetunes.feature.importing.generated.resources.icon_back
import tidetunes.feature.importing.generated.resources.icon_cloud
import tidetunes.feature.importing.generated.resources.icon_file
import tidetunes.feature.importing.generated.resources.icon_folder
import tidetunes.feature.importing.generated.resources.icon_image
import tidetunes.feature.importing.generated.resources.icon_music_note
import tidetunes.feature.importing.generated.resources.icon_toggle_all
import tidetunes.feature.importing.generated.resources.icon_warning
import tidetunes.feature.importing.generated.resources.icon_yes
import tidetunes.feature.importing.generated.resources.import_library_title
import tidetunes.feature.importing.generated.resources.import_musics_error_authentication_desc
import tidetunes.feature.importing.generated.resources.import_musics_error_authentication_title
import tidetunes.feature.importing.generated.resources.import_musics_error_permission_desc
import tidetunes.feature.importing.generated.resources.import_musics_error_permission_title
import tidetunes.feature.importing.generated.resources.import_musics_error_timeout_desc
import tidetunes.feature.importing.generated.resources.import_musics_error_timeout_title
import tidetunes.feature.importing.generated.resources.import_musics_error_unknown_desc
import tidetunes.feature.importing.generated.resources.import_musics_error_unknown_title
import tidetunes.feature.importing.generated.resources.import_musics_paths_root
import tidetunes.feature.importing.generated.resources.import_musics_title_default
import tidetunes.feature.importing.generated.resources.import_musics_title_multi_suffix
import tidetunes.feature.importing.generated.resources.import_musics_title_single_suffix

@Composable
private fun ImportEntriesSkeleton() {
    @Composable
    fun Block(
        width: Dp,
        height: Dp,
    ) {
        val color = MaterialTheme.colorScheme.surfaceVariant
        Box(modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
        )
    }

    @Composable
    fun FolderItem() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(30.dp)
        ) {
            Block(width = 30.dp, height = 30.dp)
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Block(width = 138.dp, height = 17.dp)
                Block(width = 45.dp, height = 9.dp)
            }
        }
    }

    @Composable
    fun FileItem() {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Block(width = 30.dp, height = 30.dp)
                Block(width = 138.dp, height = 17.dp)
            }
            Block(width = 16.dp, height = 16.dp)
        }
    }


    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(28.dp, 28.dp)
    ) {
        Block(
            width = 144.dp,
            height = 17.dp
        )
        FolderItem()
        FileItem()
        FileItem()
    }
}

@Composable
private fun ImportEntry(
    entry: SourceNode,
    checked: Boolean,
    allowNodeTypes: List<SourceNodeType>,
    onClickEntry: (entry: SourceNode) -> Unit
) {
    val canCheck = allowNodeTypes.any { type -> type == entry.type }
    val painter = when (entry.type) {
        SourceNodeType.Folder -> painterResource(Res.drawable.icon_folder)
        SourceNodeType.Image -> painterResource(Res.drawable.icon_image)
        SourceNodeType.Track -> painterResource(Res.drawable.icon_music_note)
        else -> painterResource(Res.drawable.icon_file)
    }
    val onClick = {
        onClickEntry(entry)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(0.dp, 8.dp)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.0F)
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
            )
            Text(
                text = entry.name,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(16.dp)
        ) {
            if (canCheck) {
                TideTunesCheckbox(
                    value = checked,
                    onChange = {
                        onClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ImportEntries(
    state: ImportState,
    onAction: (ImportAction) -> Unit,
) {
    @Composable
    fun PathTab(
        text: String,
        path: String,
        disabled: Boolean,
    ) {
        val color = if (!disabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(
                    enabled = !disabled,
                    onClick = {
                        onAction(ImportAction.OpenPath(path))
                    }
                )
                .clip(RoundedCornerShape(2.dp))
                .widthIn(10.dp, 100.dp)
                .padding(4.dp, 2.dp)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(28.dp, 8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                PathTab(
                    text = stringResource(Res.string.import_musics_paths_root),
                    path = "/",
                    disabled = state.splitPaths.isEmpty()
                )
                for ((index, v) in state.splitPaths.withIndex()) {
                    Text(
                        text = ">",
                        fontSize = 10.sp,
                    )
                    PathTab(
                        text = v.name,
                        path = v.path,
                        disabled = index == state.splitPaths.size - 1,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .padding(28.dp, 0.dp)
            ) {
                items(state.entries, key = { item -> item.path }) {
                    ImportEntry(
                        entry = it,
                        checked = state.selectedPaths.contains(it.path),
                        allowNodeTypes = state.allowNodeTypes,
                        onClickEntry = { entry ->
                            onAction(ImportAction.OpenEntry(entry))
                        },
                    )
                }
                item {
                    Box(modifier = Modifier.height(12.dp))
                }
            }
        }
        if (state.selectionMode == ImportSelectionMode.CurrentDirectory) {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.surface,
                onClick = {
                    onAction(ImportAction.FinishCurrentDirectory)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset((-40).dp, (-40).dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_yes),
                    contentDescription = null,
                )
            }
        } else if (state.selectedCount > 0) {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.surface,
                onClick = {
                    onAction(ImportAction.FinishSelection)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset((-40).dp, (-40).dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_yes),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ImportStorages(
    state: ImportState,
    onAction: (ImportAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(28.dp, 0.dp)
            .horizontalScroll(rememberScrollState())
    ) {
       for (item in state.storageAccounts) {
            val selected = state.selectedStorageAccountId == item.accountId

            val bgColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val textColor = if (selected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onAction(ImportAction.SelectStorage(item.accountId))
                    }
                    .background(bgColor)
                    .width(142.dp)
                    .height(65.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp, 16.dp)
                ) {
                    Text(
                        text = item.name,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.subtitle,
                        color = textColor,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!item.isLocal) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_cloud),
                        contentDescription = null,
                        tint = Color.Black.copy(0.2F),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .width(27.dp)
                            .offset(7.dp, 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportMusicsWarningImpl(
    title: String,
    subTitle: String,
    color: Color,
    iconPainter: Painter,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    onClick()
                }
                .padding(10.dp)
        ) {
            Box(modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                color = color,
            )
            Text(
                text = subTitle,
                fontSize = 12.sp,
                modifier = Modifier
                    .widthIn(0.dp, 220.dp)
            )
        }
    }
}

@Composable
private fun ImportMusicsError(
    loadState: ImportLoadState,
    onAction: (ImportAction) -> Unit,
) {
    val title = when (loadState) {
        ImportLoadState.AuthenticationFailed -> stringResource(Res.string.import_musics_error_authentication_title)
        ImportLoadState.Timeout -> stringResource(Res.string.import_musics_error_timeout_title)
        ImportLoadState.UnknownError -> stringResource(Res.string.import_musics_error_unknown_title)
        ImportLoadState.NeedsPermission -> stringResource(Res.string.import_musics_error_permission_title)
        ImportLoadState.Loading,
        ImportLoadState.Ready -> {
            throw RuntimeException("unsupported type")
        }
    }
    val desc = when (loadState) {
        ImportLoadState.AuthenticationFailed -> stringResource(Res.string.import_musics_error_authentication_desc)
        ImportLoadState.Timeout -> stringResource(Res.string.import_musics_error_timeout_desc)
        ImportLoadState.UnknownError -> stringResource(Res.string.import_musics_error_unknown_desc)
        ImportLoadState.NeedsPermission -> stringResource(Res.string.import_musics_error_permission_desc)
        ImportLoadState.Loading,
        ImportLoadState.Ready -> {
            throw RuntimeException("unsupported type")
        }
    }

    ImportMusicsWarningImpl(
        title = title,
        subTitle = desc,
        color = MaterialTheme.colorScheme.error,
        iconPainter = painterResource(Res.drawable.icon_warning),
        onClick = {
            onAction(ImportAction.RecoverFromLoadError)
        }
    )
}

@Composable
fun ImportScreen(
    state: ImportState,
    onAction: (ImportAction) -> Unit,
) {
    val titleText = if (state.selectionMode == ImportSelectionMode.CurrentDirectory) {
        stringResource(Res.string.import_library_title)
    } else {
        when (state.selectedCount) {
            0 -> stringResource(Res.string.import_musics_title_default)
            1 -> "${state.selectedCount} ${stringResource(Res.string.import_musics_title_single_suffix)}"
            else -> "${state.selectedCount} ${stringResource(Res.string.import_musics_title_multi_suffix)}"
        }
    }

    TideTunesBackHandler(enabled = state.canUndo) {
        onAction(ImportAction.NavigateBack)
    }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(13.dp, 13.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_back),
                    onClick = {
                        onAction(ImportAction.NavigateBack)
                    }
                )
                Text(
                    text = titleText
                )
            }
            if (state.selectionMode == ImportSelectionMode.Entries) {
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_toggle_all),
                    disabled = state.disabledToggleAll,
                    onClick = {
                        onAction(ImportAction.ToggleAll)
                    }
                )
            }
        }
        ImportStorages(
            state = state,
            onAction = onAction,
        )
        when (state.loadState) {
            ImportLoadState.Loading -> ImportEntriesSkeleton()
            ImportLoadState.Timeout,
            ImportLoadState.AuthenticationFailed,
            ImportLoadState.UnknownError,
            ImportLoadState.NeedsPermission -> ImportMusicsError(
                loadState = state.loadState,
                onAction = onAction,
            )
            ImportLoadState.Ready -> {
                ImportEntries(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}
