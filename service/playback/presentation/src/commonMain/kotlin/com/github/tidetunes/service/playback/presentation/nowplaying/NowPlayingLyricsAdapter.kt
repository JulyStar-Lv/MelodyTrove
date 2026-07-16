package com.github.tidetunes.service.playback.presentation.nowplaying

import com.github.tidetunes.core.domain.model.LyricLine
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/** Converts TideTunes timestamped lyric lines into the accompanist lyrics-core timeline model. */
internal fun List<LyricLine>.toSyncedLyrics(
    trackTitle: String,
    trackDurationMs: Long?,
): SyncedLyrics {
    val timeline = mapIndexed { index, line ->
        val startMs = line.duration.inWholeMilliseconds.toSafeInt().coerceAtLeast(0)
        val nextStartMs = getOrNull(index + 1)
            ?.duration
            ?.inWholeMilliseconds
            ?.toSafeInt()
        val fallbackEndMs = if (index == lastIndex) {
            trackDurationMs?.toSafeInt()
        } else {
            nextStartMs
        }
        val endMs = fallbackEndMs
            ?.coerceAtLeast(startMs + 1)
            ?: (startMs + 5_000)

        line.toSyncedLine(startMs = startMs, endMs = endMs)
    }

    return SyncedLyrics(
        lines = timeline,
        title = trackTitle,
    )
}

private fun LyricLine.toSyncedLine(startMs: Int, endMs: Int): ISyncedLine {
    if (words.isEmpty()) {
        return SyncedLine(
            content = text,
            translation = null,
            start = startMs,
            end = endMs,
        )
    }

    val syllables = words.mapIndexed { index, word ->
        val wordStart = (startMs.toLong() + word.startOffset.inWholeMilliseconds)
            .toSafeInt()
            .coerceIn(startMs, endMs)
        val wordEnd = (wordStart.toLong() + word.duration.inWholeMilliseconds)
            .toSafeInt()
            .coerceIn(wordStart, endMs)
        KaraokeSyllable(
            content = word.text + wordSeparatorAfter(index),
            start = wordStart,
            end = wordEnd,
        )
    }

    return KaraokeLine.MainKaraokeLine(
        syllables = syllables,
        translation = null,
        alignment = KaraokeAlignment.Start,
        start = startMs,
        end = endMs,
    )
}

private fun LyricLine.wordSeparatorAfter(index: Int): String {
    if (index >= words.lastIndex || text.none(Char::isWhitespace)) return ""
    val currentEndsWithSpace = words[index].text.lastOrNull()?.isWhitespace() == true
    val nextStartsWithSpace = words[index + 1].text.firstOrNull()?.isWhitespace() == true
    return if (currentEndsWithSpace || nextStartsWithSpace) "" else " "
}

private fun Long.toSafeInt(): Int = coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
