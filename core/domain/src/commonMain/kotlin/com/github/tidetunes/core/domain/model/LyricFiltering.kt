package com.github.tidetunes.core.domain.model

/** Shared filtering used by the full lyrics page, player lyrics, and platform outputs. */
fun LyricDisplaySettings.isLyricLineVisible(rawText: String): Boolean {
    val text = rawText.trim()
    if (text.isEmpty()) return false
    if (lineBlacklist.any { blocked -> blocked.trim() == text }) return false
    return !ignoreHeaderTags || !LYRIC_HEADER_TAG.matches(text)
}

fun LyricDisplaySettings.filterLyricTextBlock(content: String): List<String> =
    content.lineSequence().filter(::isLyricLineVisible).toList()

private val LYRIC_HEADER_TAG = Regex(
    pattern = """^\[(ar|artist|al|album|ti|title|au|author|by|offset|length|re|ve|tool|provider|id|language|la):.*]$""",
    option = RegexOption.IGNORE_CASE,
)
