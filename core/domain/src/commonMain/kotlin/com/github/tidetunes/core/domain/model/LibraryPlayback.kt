package com.github.tidetunes.core.domain.model

/**
 * Synthetic playlist id used by playback to represent the whole Library track list.
 *
 * Real playlists are persisted with positive ids, so this value is reserved for
 * in-memory playback context and must never be written as a playlist row.
 */
const val LIBRARY_PLAYBACK_PLAYLIST_ID: Long = -1L
