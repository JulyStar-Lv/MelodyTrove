package io.github.julystar.musicapp.database

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "track_fts")
@Fts4(contentEntity = TrackEntity::class)
data class TrackFts(
    val title: String,
    val artist: String,
    val albumArtist: String,
    val composer: String,
)
