package com.github.tidetunes.feature.importing.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideCheckbox
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideFab
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.LocalTideBottomContentInset
import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.core.presentation.platform.TideTunesBackHandler
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
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
private fun ImportEntriesSkeleton(
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val shapes = TideTunesTokens.shapes
    val spacing = TideTunesTokens.spacing

    @Composable
    fun Block(
        width: Dp,
        height: Dp,
    ) {
        val color = MiuixTheme.colorScheme.surfaceContainerHigh
        Box(modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(shapes.xs))
            .background(color)
        )
    }

    @Composable
    fun FolderItem() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(38.dp)
        ) {
            Block(width = 38.dp, height = 38.dp)
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
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Block(width = 38.dp, height = 38.dp)
                Block(width = 138.dp, height = 17.dp)
            }
            Block(width = 20.dp, height = 20.dp)
        }
    }


    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = spacing.md)
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

    TideCardSurface(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.0F)
            ) {
                ImportEntryIcon(painter = painter, active = canCheck)
                Text(
                    text = entry.name,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(modifier = Modifier.width(12.dp))
            if (canCheck) {
                TideCheckbox(
                    checked = checked,
                    onCheckedChange = {
                        onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun ImportEntryIcon(
    painter: Painter,
    active: Boolean,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.md)
    val tint = if (active) {
        TideTunesBrand.Primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(if (active) MiuixTheme.colorScheme.tertiaryContainer else MiuixTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, if (active) TideTunesBrand.Primary.copy(alpha = 0.18f) else MiuixTheme.colorScheme.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun ImportEntries(
    state: ImportState,
    horizontalPadding: Dp,
    onAction: (ImportAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    val shapes = TideTunesTokens.shapes
    val bottomContentInset = LocalTideBottomContentInset.current

    @Composable
    fun PathTab(
        text: String,
        path: String,
        disabled: Boolean,
    ) {
        val shape = RoundedCornerShape(shapes.full)
        val color = if (!disabled) {
            TideTunesBrand.Primary
        } else {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        }
        Text(
            text = text,
            color = color,
            style = MiuixTheme.textStyles.footnote2,
            fontWeight = if (!disabled) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(shape)
                .background(if (!disabled) MiuixTheme.colorScheme.tertiaryContainer else MiuixTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, if (!disabled) TideTunesBrand.Primary.copy(alpha = 0.16f) else MiuixTheme.colorScheme.outline, shape)
                .clickable(
                    enabled = !disabled,
                    onClick = {
                        onAction(ImportAction.OpenPath(path))
                    }
                )
                .widthIn(24.dp, 148.dp)
                .padding(horizontal = spacing.sm, vertical = spacing.xxs)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(horizontal = horizontalPadding, vertical = spacing.xs)
                    .horizontalScroll(rememberScrollState())
            ) {
                PathTab(
                    text = stringResource(Res.string.import_musics_paths_root),
                    path = "/",
                    disabled = state.splitPaths.isEmpty()
                )
                for ((index, v) in state.splitPaths.withIndex()) {
                    TideChevron(
                        size = 8.dp,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                contentPadding = PaddingValues(bottom = 88.dp + bottomContentInset),
            ) {
                itemsIndexed(state.entries, key = { index, item -> item.lazyListKey(index) }) { _, item ->
                    ImportEntry(
                        entry = item,
                        checked = state.selectedPaths.contains(item.path),
                        allowNodeTypes = state.allowNodeTypes,
                        onClickEntry = { entry ->
                            onAction(ImportAction.OpenEntry(entry))
                        },
                    )
                }
            }
        }
        if (state.selectionMode == ImportSelectionMode.CurrentDirectory) {
            TideFab(
                onClick = {
                    onAction(ImportAction.FinishCurrentDirectory)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = horizontalPadding, bottom = horizontalPadding)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_yes),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else if (state.selectedCount > 0) {
            TideFab(
                onClick = {
                    onAction(ImportAction.FinishSelection)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = horizontalPadding, bottom = horizontalPadding)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_yes),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

internal fun com.github.tidetunes.source.api.SourceNode.lazyListKey(index: Int): String =
    "import-entry-$index-$path"

@Composable
private fun ImportStorages(
    state: ImportState,
    horizontalPadding: Dp,
    onAction: (ImportAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    val shapes = TideTunesTokens.shapes

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .horizontalScroll(rememberScrollState())
    ) {
       for (item in state.storageAccounts) {
            val selected = state.selectedStorageAccountId == item.accountId
            val shape = RoundedCornerShape(shapes.lg)
            val bgColor = if (selected) {
                TideTunesBrand.Primary
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            }
            val textColor = if (selected) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSurface
            }
            val subtitleColor = if (selected) {
                MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            }
            val borderColor = if (selected) {
                TideTunesBrand.Primary.copy(alpha = 0.28f)
            } else {
                MiuixTheme.colorScheme.outline
            }

            Box(
                modifier = Modifier
                    .clip(shape)
                    .clickable {
                        onAction(ImportAction.SelectStorage(item.accountId))
                    }
                    .background(bgColor)
                    .border(1.dp, borderColor, shape)
                    .width(156.dp)
                    .heightIn(min = 76.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                    modifier = Modifier
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                ) {
                    Text(
                        text = item.name,
                        color = textColor,
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.subtitle,
                        color = subtitleColor,
                        style = MiuixTheme.textStyles.footnote2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!item.isLocal) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_cloud),
                        contentDescription = null,
                        tint = if (selected) MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else TideTunesBrand.Primary.copy(alpha = 0.18f),
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
    horizontalPadding: Dp,
    onClick: () -> Unit,
) {
    val spacing = TideTunesTokens.spacing

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontalPadding)
    ) {
        TideCardSurface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .heightIn(min = 220.dp),
            cornerRadius = TideTunesTokens.shapes.xl,
            contentPadding = PaddingValues(spacing.lg),
            onClick = onClick,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = title,
                    color = color,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subTitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(0.dp, 220.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportMusicsError(
    loadState: ImportLoadState,
    horizontalPadding: Dp,
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
        color = MiuixTheme.colorScheme.error,
        iconPainter = painterResource(Res.drawable.icon_warning),
        horizontalPadding = horizontalPadding,
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
    val spacing = TideTunesTokens.spacing
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageMedium

        Column(
            modifier = Modifier
                .background(MiuixTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = 18.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    TideIconButton(
                        size = TideIconButtonSize.Medium,
                        variant = TideIconButtonVariant.Default,
                        painter = painterResource(Res.drawable.icon_back),
                        onClick = {
                            onAction(ImportAction.NavigateBack)
                        }
                    )
                    Text(
                        text = titleText,
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.selectionMode == ImportSelectionMode.Entries) {
                    TideIconButton(
                        size = TideIconButtonSize.Medium,
                        variant = TideIconButtonVariant.Default,
                        painter = painterResource(Res.drawable.icon_toggle_all),
                        enabled = !state.disabledToggleAll,
                        onClick = {
                            onAction(ImportAction.ToggleAll)
                        }
                    )
                }
            }
            ImportStorages(
                state = state,
                horizontalPadding = horizontalPadding,
                onAction = onAction,
            )
            Box(modifier = Modifier.height(spacing.md))
            when (state.loadState) {
                ImportLoadState.Loading -> ImportEntriesSkeleton(horizontalPadding = horizontalPadding)
                ImportLoadState.Timeout,
                ImportLoadState.AuthenticationFailed,
                ImportLoadState.UnknownError,
                ImportLoadState.NeedsPermission -> ImportMusicsError(
                    loadState = state.loadState,
                    horizontalPadding = horizontalPadding,
                    onAction = onAction,
                )
                ImportLoadState.Ready -> {
                    ImportEntries(
                        state = state,
                        horizontalPadding = horizontalPadding,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}
