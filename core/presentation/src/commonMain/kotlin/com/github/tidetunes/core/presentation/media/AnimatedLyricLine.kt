package com.github.tidetunes.core.presentation.media

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.domain.model.LyricLine
import com.github.tidetunes.core.domain.model.LyricWord
import kotlin.time.Duration

/**
 * Position within a single lyric line for word-by-word animation.
 */
enum class LyricWordState {
    /** Word has not yet been reached. */
    Inactive,
    /** Word is currently being sung. */
    Active,
    /** Word has been completed. */
    Completed,
}

/**
 * Animation target colour for each word state.
 */
@Composable
private fun activeColor() = MaterialTheme.colorScheme.primary

@Composable
private fun completedColor() = MaterialTheme.colorScheme.onSurface

@Composable
private fun inactiveColor() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

/**
 * Animated lyric line that highlights individual words as playback progresses.
 *
 * When [linePositionMs] is provided and the [line] carries word-timing data,
 * each word transitions through Inactive → Active → Completed states as the
 * playback position passes through its time window.
 *
 * Without word-timing data, the entire line fades to completed color when
 * [isCurrentLine] is true and [linePositionMs] exceeds half the line duration.
 */
@Composable
fun AnimatedLyricLine(
    line: LyricLine,
    isCurrentLine: Boolean,
    linePositionMs: Duration,
    modifier: Modifier = Modifier,
) {
    val hasWords = line.words.isNotEmpty()

    if (!hasWords) {
        // Simple line: fade entire line to completed as it progresses
        PlainLyricLine(
            text = line.text,
            isCurrentLine = isCurrentLine,
            isActive = isCurrentLine && linePositionMs < line.duration / 2,
            modifier = modifier,
        )
        return
    }

    // Word-by-word rendering
    val linePosMs = linePositionMs.inWholeMilliseconds
    val fontSize = if (isCurrentLine) 18.sp else 14.sp
    val inactiveWordColor = inactiveColor()
    val activeWordColor = activeColor()
    val completedWordColor = completedColor()

    val annotated = buildAnnotatedString {
        line.words.forEachIndexed { index, word ->
            val state = wordState(word, linePosMs)
            val color = when (state) {
                LyricWordState.Inactive -> inactiveWordColor
                LyricWordState.Active -> activeWordColor
                LyricWordState.Completed -> completedWordColor
            }
            val weight = if (state == LyricWordState.Active) FontWeight.Bold else FontWeight.Normal
            withStyle(SpanStyle(color = color, fontWeight = weight, fontSize = fontSize)) {
                append(word.text)
            }
            // Add space between words (but not after the last)
            if (index < line.words.size - 1) {
                withStyle(SpanStyle(color = color)) {
                    append(" ")
                }
            }
        }
    }

    Text(
        text = annotated,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
    )
}

/**
 * Determines the state of a word based on the current line position.
 */
private fun wordState(word: LyricWord, linePositionMs: Long): LyricWordState {
    val wordStartMs = word.startOffset.inWholeMilliseconds
    val wordEndMs = wordStartMs + word.duration.inWholeMilliseconds

    return when {
        linePositionMs < wordStartMs -> LyricWordState.Inactive
        linePositionMs < wordEndMs -> LyricWordState.Active
        else -> LyricWordState.Completed
    }
}

/**
 * Renders a plain text lyric line without word-by-word animation.
 */
@Composable
private fun PlainLyricLine(
    text: String,
    isCurrentLine: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = when {
        !isCurrentLine -> inactiveColor()
        !isActive -> completedColor()
        else -> activeColor()
    }
    val weight = if (isActive) FontWeight.Bold else FontWeight.Normal
    val fontSize = if (isCurrentLine) 18.sp else 14.sp

    Text(
        text = text,
        color = color,
        fontWeight = weight,
        fontSize = fontSize,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
    )
}
