package io.github.julystar.musicapp.core.domain.model

import kotlinx.collections.immutable.toPersistentList

/** Shared filtering used by the full lyrics page, player lyrics, and platform outputs. */
fun LyricDisplaySettings.isLyricLineVisible(rawText: String): Boolean {
    val text = rawText.trim().removePrefix("\uFEFF").trimStart()
    if (text.isEmpty()) return false
    if (lineBlacklist.any { blocked -> blocked.trim() == text }) return false
    return !ignoreHeaderTags || !LYRIC_HEADER_TAG.matches(text)
}

fun LyricDisplaySettings.filterLyricTextBlock(content: String): List<String> =
    content.lineSequence().filter(::isLyricLineVisible).toList()

/** Keeps platform lyric outputs in sync with the filtering used by the in-app player. */
fun Lyrics.filteredForDisplay(settings: LyricDisplaySettings): Lyrics {
    val containsUnsynchronisedBlock = lines.size == 1 &&
        lines.first().words.isEmpty() &&
        lines.first().duration.inWholeMilliseconds == 0L
    val visibleLines = if (containsUnsynchronisedBlock) {
        val line = lines.first()
        settings.filterLyricTextBlock(line.text)
            .takeIf { visibleText -> visibleText.isNotEmpty() }
            ?.let { visibleText -> listOf(line.copy(text = visibleText.joinToString("\n"))) }
            .orEmpty()
    } else {
        lines.filter { line ->
            val primary = line.text.lineSequence().firstOrNull(String::isNotBlank).orEmpty()
            settings.isLyricLineVisible(primary)
        }
    }
    return copy(lines = visibleLines.toPersistentList())
}

private val LYRIC_HEADER_TAG = Regex(
    pattern = """^\[\s*(ar|artist|al|album|ti|title|au|author|by|offset|length|re|ve|tool|provider|id|language|la|encoding|enc|source|src|kana)\s*[:：].*]$""",
    option = RegexOption.IGNORE_CASE,
)
