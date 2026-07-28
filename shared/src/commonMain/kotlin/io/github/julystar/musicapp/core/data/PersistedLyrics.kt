package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.LyricLine
import io.github.julystar.musicapp.core.domain.model.LyricDisplaySettings
import io.github.julystar.musicapp.core.domain.model.LyricSourceKind
import io.github.julystar.musicapp.core.domain.model.LyricSourceMode
import io.github.julystar.musicapp.core.domain.model.LyricWord
import io.github.julystar.musicapp.core.domain.model.Lyrics
import io.github.julystar.musicapp.core.domain.model.LyricsLoadState
import io.github.julystar.musicapp.database.LyricsEntity
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

internal fun List<LyricsEntity>.selectLyrics(settings: LyricDisplaySettings): LyricsEntity? {
    val eligible = filter { entity ->
        when (settings.sourceMode) {
            LyricSourceMode.Auto -> true
            LyricSourceMode.Embedded -> entity.resolvedSourceKind().isEmbedded
            LyricSourceMode.External -> !entity.resolvedSourceKind().isEmbedded
        }
    }
    val priority = settings.sourcePriority.withIndex().associate { (index, kind) -> kind to index }
    return eligible.minWithOrNull(
        compareBy<LyricsEntity> { entity -> priority[entity.resolvedSourceKind()] ?: Int.MAX_VALUE }
            .thenByDescending(LyricsEntity::updatedAt),
    )
}

internal fun LyricsEntity.resolvedSourceKind(): LyricSourceKind =
    runCatching { LyricSourceKind.valueOf(sourceKind) }.getOrElse {
        val embedded = sourcePath.isNullOrBlank() || sourcePath.startsWith("embedded", ignoreCase = true)
        val ttml = format.equals("TTML", ignoreCase = true)
        when {
            embedded && ttml -> LyricSourceKind.EmbeddedTtml
            embedded -> LyricSourceKind.EmbeddedPlain
            ttml -> LyricSourceKind.ExternalTtml
            else -> LyricSourceKind.ExternalPlain
        }
    }

private val LyricSourceKind.isEmbedded: Boolean
    get() = this == LyricSourceKind.EmbeddedTtml || this == LyricSourceKind.EmbeddedPlain

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
