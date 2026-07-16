package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.LyricLine
import com.github.tidetunes.core.domain.model.LyricWord
import com.github.tidetunes.core.domain.model.Lyrics
import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.database.LyricsEntity
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Duration.Companion.milliseconds

internal fun LyricsEntity.toPlaybackLyrics(): Lyrics {
    if (!synchronized) {
        return Lyrics(
            lines = persistentListOf(LyricLine(0.milliseconds, content)),
            loadState = LyricsLoadState.Loaded,
        )
    }

    val parsedLines = runCatching { AutoParser().parse(content).lines }
        .getOrDefault(emptyList())
        .mapNotNull { line ->
            when (line) {
                is KaraokeLine -> line.toDomainLine()
                is SyncedLine -> LyricLine(line.start.milliseconds, line.content)
                is UncheckedSyncedLine -> LyricLine(line.start.milliseconds, line.content)
                else -> null
            }
        }
        .ifEmpty {
            content.lineSequence().mapNotNull(::parseBasicLrcLine).toList()
        }

    return Lyrics(
        lines = parsedLines.toPersistentList(),
        loadState = LyricsLoadState.Loaded,
    )
}

private fun parseBasicLrcLine(rawLine: String): LyricLine? {
    val line = rawLine.trim()
    if (!line.startsWith("[")) return null
    val close = line.indexOf(']')
    if (close <= 1) return null
    val timestamp = line.substring(1, close).split(':')
    if (timestamp.size != 2) return null
    val minutes = timestamp[0].toLongOrNull() ?: return null
    val seconds = timestamp[1].toDoubleOrNull() ?: return null
    val text = line.substring(close + 1).trim()
    if (text.isEmpty()) return null
    return LyricLine(
        duration = (minutes * 60_000L + (seconds * 1_000.0).toLong()).milliseconds,
        text = text,
    )
}

private fun KaraokeLine.toDomainLine(): LyricLine {
    val lineStart = start.coerceAtLeast(0)
    return LyricLine(
        duration = lineStart.milliseconds,
        text = syllables.joinToString(separator = "") { syllable -> syllable.content },
        words = syllables.map { syllable ->
            LyricWord(
                text = syllable.content,
                startOffset = (syllable.start - lineStart).coerceAtLeast(0).milliseconds,
                duration = (syllable.end - syllable.start).coerceAtLeast(1).milliseconds,
            )
        }.toPersistentList(),
    )
}
