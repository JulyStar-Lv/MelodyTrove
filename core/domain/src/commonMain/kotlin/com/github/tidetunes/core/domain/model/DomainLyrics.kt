package com.github.tidetunes.core.domain.model

data class DomainLyrics(
    val trackTitle: String,
    val trackArtist: String?,
    val lines: List<String>,
    val format: String?,
    val synchronized: Boolean,
)
