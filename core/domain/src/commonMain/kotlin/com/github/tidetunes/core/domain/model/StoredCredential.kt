package com.github.tidetunes.core.domain.model

data class StoredCredential(
    val username: String,
    val secret: String,
    val isAnonymous: Boolean,
)
