package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.LyricDisplaySettings
import com.github.tidetunes.core.domain.model.LyricFontChoice
import com.github.tidetunes.core.domain.model.LyricTextAlignment
import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.core.lyrics.ui.TideLyricsView
import com.github.tidetunes.core.presentation.components.TideContextMenu
import com.github.tidetunes.core.presentation.components.TideContextMenuItem
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonColors
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.theme.TideTunesFontFamilies
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingAction
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.service.playback.presentation.nowplaying.toSyncedLyrics
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.service.playback.presentation.generated.resources.Res
import tidetunes.service.playback.presentation.generated.resources.icon_back
import tidetunes.service.playback.presentation.generated.resources.icon_heart
import tidetunes.service.playback.presentation.generated.resources.icon_vertialcal_more
import tidetunes.service.playback.presentation.generated.resources.music_lyric_remove
import tidetunes.service.playback.presentation.generated.resources.player_add_favorite
import tidetunes.service.playback.presentation.generated.resources.player_close
import tidetunes.service.playback.presentation.generated.resources.player_loading_lyrics
import tidetunes.service.playback.presentation.generated.resources.player_remove_favorite
import tidetunes.service.playback.presentation.generated.resources.player_unknown_artist
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LyricsBackground = Color(0xFF0C0A14)

@Composable
fun LyricsScreen(
    state: LyricsState,
    nowPlayingTrack: NowPlayingTrackItem?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    lyricDisplaySettings: LyricDisplaySettings,
    onAction: (LyricsAction) -> Unit,
    onPlayerAction: (NowPlayingAction) -> Unit,
) {
    val trackTitle = nowPlayingTrack?.title ?: state.trackTitle
    val trackArtist = nowPlayingTrack?.artist ?: state.trackArtist
    val artwork = nowPlayingTrack?.artwork

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LyricsBackground)
            .clipToBounds(),
    ) {
        ArtworkImage(
            artwork = artwork,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.46f
                    scaleX = 1.14f
                    scaleY = 1.14f
                    renderEffect = BlurEffect(88f, 88f, TileMode.Decal)
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF151126).copy(alpha = 0.42f),
                            LyricsBackground.copy(alpha = 0.68f),
                            LyricsBackground.copy(alpha = 0.94f),
                        ),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            LyricsTrackHeader(
                track = nowPlayingTrack,
                title = trackTitle,
                artist = trackArtist,
                onNavigateBack = { onAction(LyricsAction.NavigateBack) },
                onPlayerAction = onPlayerAction,
            )

            if (nowPlayingTrack != null) {
                NowPlayingLyricsContent(
                    track = nowPlayingTrack,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    lyricDisplaySettings = lyricDisplaySettings,
                    onPlayerAction = onPlayerAction,
                    modifier = Modifier.weight(1f),
                )
            } else {
                StoredLyricsContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LyricsTrackHeader(
    track: NowPlayingTrackItem?,
    title: String,
    artist: String?,
    onNavigateBack: () -> Unit,
    onPlayerAction: (NowPlayingAction) -> Unit,
) {
    var liked by remember(track?.id) { mutableStateOf(true) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(84.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TideIconButton(
            size = TideIconButtonSize.Touch,
            variant = TideIconButtonVariant.Default,
            painter = painterResource(Res.drawable.icon_back),
            contentDescription = stringResource(Res.string.player_close),
            colors = TideIconButtonColors(iconTint = Color.White),
            onClick = onNavigateBack,
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp)),
        ) {
            ArtworkImage(
                artwork = track?.artwork,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MiuixTheme.textStyles.title3.copy(fontSize = 19.sp, lineHeight = 23.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist ?: stringResource(Res.string.player_unknown_artist),
                color = Color.White.copy(alpha = 0.62f),
                style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 19.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (track != null) {
            TideIconButton(
                size = TideIconButtonSize.Touch,
                variant = TideIconButtonVariant.Default,
                painter = painterResource(Res.drawable.icon_heart),
                contentDescription = stringResource(
                    if (liked) Res.string.player_remove_favorite else Res.string.player_add_favorite,
                ),
                colors = lyricsHeaderButtonColors(
                    iconTint = if (liked) MiuixTheme.colorScheme.primary else Color.White,
                ),
                onClick = { liked = !liked },
            )
            Box {
                TideIconButton(
                    size = TideIconButtonSize.Touch,
                    variant = TideIconButtonVariant.Default,
                    painter = painterResource(Res.drawable.icon_vertialcal_more),
                    contentDescription = null,
                    colors = lyricsHeaderButtonColors(),
                    onClick = { moreMenuExpanded = true },
                )
                TideContextMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                    items = listOf(
                        TideContextMenuItem(
                            label = Res.string.music_lyric_remove,
                            onClick = {
                                moreMenuExpanded = false
                                onPlayerAction(NowPlayingAction.RemoveLyric)
                            },
                        ),
                    ),
                )
            }
        }
    }
}

private fun lyricsHeaderButtonColors(iconTint: Color = Color.White): TideIconButtonColors =
    TideIconButtonColors(
        buttonBg = Color.White.copy(alpha = 0.10f),
        iconTint = iconTint,
    )

@Composable
private fun NowPlayingLyricsContent(
    track: NowPlayingTrackItem,
    currentPositionMs: Long,
    isPlaying: Boolean,
    lyricDisplaySettings: LyricDisplaySettings,
    onPlayerAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadState = track.lyrics.loadState
    val lyricLines = track.lyrics.lines
    val syncedLyrics = remember(lyricLines, track.title, track.durationMs, lyricDisplaySettings) {
        lyricLines.toSyncedLyrics(
            trackTitle = track.title,
            trackDurationMs = track.durationMs,
            settings = lyricDisplaySettings,
        )
    }

    when {
        loadState == LyricsLoadState.Loading -> LyricsStatus(
            message = stringResource(Res.string.player_loading_lyrics),
            modifier = modifier,
        )
        loadState == LyricsLoadState.Missing || loadState == LyricsLoadState.Failed || syncedLyrics.lines.isEmpty() ->
            LyricsStatus(
                message = "No lyrics available",
                modifier = modifier,
            )
        else -> {
            val primarySize = lyricDisplaySettings.primaryFontSizeSp *
                (lyricDisplaySettings.primaryFontScalePercent / 100f) * 0.875f
            val secondarySize = lyricDisplaySettings.secondaryFontSizeSp *
                (lyricDisplaySettings.secondaryFontScalePercent / 100f) * (14f / 19f)
            val textAlign = when (lyricDisplaySettings.textAlignment) {
                LyricTextAlignment.Left -> TextAlign.Start
                LyricTextAlignment.Center -> TextAlign.Center
                LyricTextAlignment.Right -> TextAlign.End
            }
            val fontFamily = lyricDisplaySettings.lyricPageFontFamily(lyricLines.map { it.text })

            TideLyricsView(
                lyrics = syncedLyrics,
                currentPositionMs = currentPositionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                isPlaying = isPlaying,
                onLineClick = { line ->
                    onPlayerAction(NowPlayingAction.SeekTo(line.start.coerceAtLeast(0).toULong()))
                },
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.62f),
                edgeColor = LyricsBackground.copy(alpha = 0.82f),
                activeTextStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = primarySize.sp,
                    lineHeight = (primarySize * 1.25f).sp,
                    fontWeight = FontWeight.Bold,
                ),
                inactiveTextStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (primarySize * (23f / 28f)).sp,
                    lineHeight = (primarySize * 1.30f * (23f / 28f)).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                secondaryTextStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = secondarySize.sp,
                    lineHeight = (secondarySize * 1.42f).sp,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = textAlign,
                lineSpacing = 22.dp,
                showTranslation = lyricDisplaySettings.showTranslation,
                wordLiftEnabled = lyricDisplaySettings.wordLiftEnabled,
                useBlurEffect = lyricDisplaySettings.blurEffectEnabled,
                perspectiveEffectEnabled = lyricDisplaySettings.perspectiveEffectEnabled,
                perspectiveAngleDegrees = lyricDisplaySettings.perspectiveAngleDegrees.toFloat(),
                tapToSeekEnabled = lyricDisplaySettings.tapToSeekEnabled,
                modifier = modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StoredLyricsContent(
    state: LyricsState,
    onAction: (LyricsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LyricsStatus("Loading lyrics", modifier)
        state.error != null -> LyricsStatus(state.error, modifier, onRetry = { onAction(LyricsAction.Retry) })
        state.lines.isEmpty() -> LyricsStatus("No lyrics available", modifier)
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Spacer(Modifier.height(120.dp)) }
            items(state.lines) { line ->
                Text(
                    text = line,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MiuixTheme.textStyles.title3.copy(fontSize = 23.sp, lineHeight = 30.sp),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 11.dp),
                )
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun LyricsStatus(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.62f),
            style = MiuixTheme.textStyles.body1,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            TideTextButton(
                text = "Retry",
                variant = TideTextButtonVariant.Primary,
                size = TideTextButtonSize.Medium,
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun LyricDisplaySettings.lyricPageFontFamily(lines: List<String>): FontFamily? {
    if (!font.applyToLyricsPage) return null
    val containsCjk = lines.any { line -> line.any(Char::isCjkCharacter) }
    return when (if (containsCjk) font.cjkFont else font.westernFont) {
        LyricFontChoice.System -> FontFamily.Default
        LyricFontChoice.TideSans -> TideTunesFontFamilies.JakartaSans
        LyricFontChoice.TideCjk -> TideTunesFontFamilies.Sans
        LyricFontChoice.Monospace -> TideTunesFontFamilies.Mono
    }
}

private fun Char.isCjkCharacter(): Boolean = code in 0x2E80..0x9FFF ||
    code in 0xAC00..0xD7AF ||
    code in 0xF900..0xFAFF
