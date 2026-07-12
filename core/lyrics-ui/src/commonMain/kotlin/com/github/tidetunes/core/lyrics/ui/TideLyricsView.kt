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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlin.math.abs

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
                    currentPositionMs = currentPositionMs,
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
    currentPositionMs: Int,
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
    val content = remember(line, currentPositionMs, activeColor, inactiveColor, isCurrent) {
        line.toAnnotatedString(
            currentPositionMs = currentPositionMs,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            isCurrent = isCurrent,
        )
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
        BasicText(
            text = content,
            style = if (isCurrent) activeTextStyle else inactiveTextStyle,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
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

private fun ISyncedLine.toAnnotatedString(
    currentPositionMs: Int,
    activeColor: Color,
    inactiveColor: Color,
    isCurrent: Boolean,
): AnnotatedString {
    if (this !is KaraokeLine) {
        val text = (this as? SyncedLine)?.content.orEmpty()
        return AnnotatedString(text, spanStyle = SpanStyle(color = if (isCurrent) activeColor else inactiveColor))
    }

    return buildAnnotatedString {
        syllables.forEach { syllable ->
            val color = when {
                !isCurrent -> inactiveColor
                currentPositionMs >= syllable.end -> activeColor
                currentPositionMs <= syllable.start -> inactiveColor
                else -> lerp(inactiveColor, activeColor, syllable.progress(currentPositionMs))
            }
            withStyle(SpanStyle(color = color)) {
                append(syllable.content)
            }
        }
    }
}
