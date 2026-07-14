package com.github.tidetunes.source.api

data class MetaSongQuery(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val config: Map<String, String> = emptyMap(),
    val keyword: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
    val separator: String = "/",
)

data class MetaSongCandidate(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val date: String? = null,
    val trackNumber: String? = null,
    val pictureUrl: String? = null,
    val fields: Map<String, String> = emptyMap(),
    val contextToken: String? = null,
)

data class MetaCoverCandidate(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val sourceId: String? = null,
)

data class MetaLyricWord(
    val text: String,
    val startMs: Long? = null,
    val endMs: Long? = null,
)

data class MetaLyricLine(
    val text: String,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val words: List<MetaLyricWord> = emptyList(),
    val translation: String? = null,
    val romanization: String? = null,
    val person: String? = null,
)

data class MetaLyrics(
    val lines: List<MetaLyricLine> = emptyList(),
    val rawPlainLrc: String? = null,
    val rawVerbatimLrc: String? = null,
    val rawEnhancedLrc: String? = null,
    val rawTtml: String? = null,
    val rawMultiPersonEnhancedLrc: String? = null,
    val translated: String? = null,
    val romanization: String? = null,
)

interface MetaSource {
    val id: String
    val displayName: String

    suspend fun searchSongs(query: MetaSongQuery): List<MetaSongCandidate>

    suspend fun getLyrics(
        candidate: MetaSongCandidate,
        config: Map<String, String> = emptyMap(),
    ): MetaLyrics?

    suspend fun searchCovers(query: MetaSongQuery): List<MetaCoverCandidate>
}

class MetaSourceRegistry(sources: Collection<MetaSource> = emptyList()) {
    private val byId = sources.associateBy { it.id }
        .also { require(it.size == sources.size) { "MetaSource IDs must be unique" } }

    val sources: Collection<MetaSource> get() = byId.values

    fun sourceOrNull(id: String) = byId[id]
}
