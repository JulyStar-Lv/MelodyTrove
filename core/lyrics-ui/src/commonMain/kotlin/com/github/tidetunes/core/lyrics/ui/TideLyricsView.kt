package com.github.tidetunes.core.lyrics.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToLong

private const val PlaybackResyncThresholdMs = 220.0
private const val PlaybackJitterToleranceMs = 24.0
private const val PlaybackCorrectionFraction = 0.25

/**
 * A desktop-friendly lyrics surface adapted from accompanist-lyrics-ui.
 *
 * The active line follows playback automatically, karaoke syllables fill according to their own
 * time ranges, nearby lines retain context, and distant lines recede through alpha and blur.
 */
@Composable
fun TideLyricsView(
    lyrics: SyncedLyrics,
    currentPositionMs: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onLineClick: (ISyncedLine) -> Unit = {},
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.34f),
    edgeColor: Color = Color.Transparent,
    activeTextStyle: TextStyle = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
    ),
    inactiveTextStyle: TextStyle = TextStyle(
        fontSize = 27.sp,
        lineHeight = 35.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    lineSpacing: Dp = 18.dp,
    useBlurEffect: Boolean = true,
) {
    val listState = rememberLazyListState()
    val renderPositionProvider = rememberInterpolatedPlaybackPositionProvider(
        currentPositionMs = currentPositionMs,
        isPlaying = isPlaying,
    )
    val currentIndex = remember(lyrics.lines, currentPositionMs) {
        lyrics.lines.indexOfLast { line -> currentPositionMs >= line.start }
            .coerceAtLeast(0)
            .coerceAtMost((lyrics.lines.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(currentIndex, lyrics.lines.size) {
        if (lyrics.lines.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .fillMaxSize(),
    ) {
        val verticalPadding = maxHeight * 0.34f

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(lineSpacing),
        ) {
            itemsIndexed(
                items = lyrics.lines,
                key = { index, line -> "${line.start}:${line.end}:$index" },
            ) { index, line ->
                val distance = abs(index - currentIndex)
                val isCurrent = index == currentIndex
                LyricLineItem(
                    line = line,
                    renderPositionProvider = renderPositionProvider.takeIf { isCurrent },
                    isCurrent = isCurrent,
                    distanceFromCurrent = distance,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    activeTextStyle = activeTextStyle,
                    inactiveTextStyle = inactiveTextStyle,
                    useBlurEffect = useBlurEffect,
                    onClick = { onLineClick(line) },
                )
            }
        }

        if (edgeColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Brush.verticalGradient(listOf(edgeColor, Color.Transparent))),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, edgeColor))),
            )
        }
    }
}

@Composable
private fun LyricLineItem(
    line: ISyncedLine,
    renderPositionProvider: (() -> Int)?,
    isCurrent: Boolean,
    distanceFromCurrent: Int,
    activeColor: Color,
    inactiveColor: Color,
    activeTextStyle: TextStyle,
    inactiveTextStyle: TextStyle,
    useBlurEffect: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.94f,
        animationSpec = spring(stiffness = 420f),
        label = "lyricLineScale",
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            isCurrent -> 1f
            distanceFromCurrent == 1 -> 0.62f
            distanceFromCurrent == 2 -> 0.42f
            else -> 0.26f
        },
        label = "lyricLineAlpha",
    )
    val blurRadius = if (useBlurEffect && distanceFromCurrent > 1) {
        (distanceFromCurrent.coerceAtMost(4) - 1) * 1.15f
    } else {
        0f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                renderEffect = if (blurRadius > 0f) {
                    BlurEffect(blurRadius, blurRadius, TileMode.Decal)
                } else {
                    null
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        KaraokeText(
            line = line,
            renderPositionProvider = renderPositionProvider,
            isCurrent = isCurrent,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            textStyle = if (isCurrent) activeTextStyle else inactiveTextStyle,
        )

        if (line is SyncedLine && !line.translation.isNullOrBlank()) {
            BasicText(
                text = line.translation.orEmpty(),
                modifier = Modifier.padding(top = 5.dp),
                style = inactiveTextStyle.copy(
                    color = activeColor.copy(alpha = if (isCurrent) 0.72f else 0.48f),
                    fontSize = inactiveTextStyle.fontSize * 0.72f,
                    lineHeight = inactiveTextStyle.lineHeight * 0.78f,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun KaraokeText(
    line: ISyncedLine,
    renderPositionProvider: (() -> Int)?,
    isCurrent: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    textStyle: TextStyle,
) {
    if (line !is KaraokeLine) {
        BasicText(
            text = (line as? SyncedLine)?.content.orEmpty(),
            style = textStyle.copy(color = if (isCurrent) activeColor else inactiveColor),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    val text = remember(line) { line.syllables.joinToString(separator = "") { it.content } }
    var revealSegments by remember(line, textStyle) {
        mutableStateOf<List<KaraokeRevealSegment>>(emptyList())
    }
    val revealPath = remember(line, textStyle) { Path() }
    Box {
        BasicText(
            text = text,
            style = textStyle.copy(color = inactiveColor),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (renderPositionProvider != null) {
            BasicText(
                text = text,
                modifier = Modifier
                    .clearAndSetSemantics { }
                    .drawWithContent {
                        revealPath.reset()
                        revealPath.addRevealedSegments(
                            segments = revealSegments,
                            currentPositionMs = renderPositionProvider(),
                        )
                        clipPath(revealPath) {
                            this@drawWithContent.drawContent()
                        }
                    },
                style = textStyle.copy(color = activeColor),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layoutResult ->
                    revealSegments = line.createRevealSegments(layoutResult)
                },
            )
        }
    }
}

private data class KaraokeRevealSegment(
    val bounds: Rect,
    val startMs: Float,
    val endMs: Float,
)

private fun KaraokeLine.createRevealSegments(
    layoutResult: TextLayoutResult,
): List<KaraokeRevealSegment> = buildList {
    var textOffset = 0
    syllables.forEach { syllable ->
        val characterDurationMs = syllable.duration.toFloat() /
            syllable.content.length.coerceAtLeast(1)
        syllable.content.indices.forEach { characterIndex ->
            val offset = textOffset + characterIndex
            if (offset < layoutResult.layoutInput.text.length) {
                val characterStartMs = syllable.start + characterDurationMs * characterIndex
                add(
                    KaraokeRevealSegment(
                        bounds = layoutResult.getBoundingBox(offset),
                        startMs = characterStartMs,
                        endMs = characterStartMs + characterDurationMs,
                    ),
                )
            }
        }
        textOffset += syllable.content.length
    }
}

private fun Path.addRevealedSegments(
    segments: List<KaraokeRevealSegment>,
    currentPositionMs: Int,
) {
    segments.forEach { segment ->
        val progress = when {
            currentPositionMs >= segment.endMs -> 1f
            currentPositionMs <= segment.startMs -> 0f
            segment.endMs <= segment.startMs -> 1f
            else -> (currentPositionMs - segment.startMs) / (segment.endMs - segment.startMs)
        }
        if (progress <= 0f) return@forEach
        val bounds = segment.bounds
        addRect(
            if (progress >= 1f) {
                bounds
            } else {
                Rect(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.left + bounds.width * progress,
                    bottom = bounds.bottom,
                )
            },
        )
    }
}

@Composable
private fun rememberInterpolatedPlaybackPositionProvider(
    currentPositionMs: Int,
    isPlaying: Boolean,
): () -> Int {
    var renderedPositionMs by remember { mutableLongStateOf(currentPositionMs.toLong()) }
    val externalPosition = rememberUpdatedState(currentPositionMs.toLong())

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            snapshotFlow { externalPosition.value }.collect { positionMs ->
                renderedPositionMs = positionMs
            }
            return@LaunchedEffect
        }

        var preciseRenderedPositionMs = renderedPositionMs.toDouble()
        var observedExternalPositionMs = externalPosition.value
        var previousFrameNanos: Long? = null

        while (isActive) {
            val frameNanos = withFrameNanos { it }
            previousFrameNanos?.let { previousNanos ->
                preciseRenderedPositionMs +=
                    (frameNanos - previousNanos).coerceAtLeast(0L) / 1_000_000.0
            }

            val latestExternalPositionMs = externalPosition.value
            if (latestExternalPositionMs != observedExternalPositionMs) {
                preciseRenderedPositionMs = correctInterpolatedPlaybackPosition(
                    externalPositionMs = latestExternalPositionMs.toDouble(),
                    renderedPositionMs = preciseRenderedPositionMs,
                )
                observedExternalPositionMs = latestExternalPositionMs
            }

            val nextRenderedPositionMs = preciseRenderedPositionMs
                .roundToLong()
                .coerceIn(0L, Int.MAX_VALUE.toLong())
            if (nextRenderedPositionMs != renderedPositionMs) {
                renderedPositionMs = nextRenderedPositionMs
            }
            previousFrameNanos = frameNanos
        }
    }

    return remember {
        { renderedPositionMs.toInt() }
    }
}

internal fun correctInterpolatedPlaybackPosition(
    externalPositionMs: Double,
    renderedPositionMs: Double,
    resyncThresholdMs: Double = PlaybackResyncThresholdMs,
    jitterToleranceMs: Double = PlaybackJitterToleranceMs,
    correctionFraction: Double = PlaybackCorrectionFraction,
): Double {
    val errorMs = externalPositionMs - renderedPositionMs
    return when {
        abs(errorMs) >= resyncThresholdMs -> externalPositionMs
        abs(errorMs) <= jitterToleranceMs -> renderedPositionMs
        else -> renderedPositionMs + errorMs * correctionFraction
    }
}
