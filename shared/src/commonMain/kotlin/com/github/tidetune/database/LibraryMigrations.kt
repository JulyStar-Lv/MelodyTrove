package com.github.tidetune.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        trackV2Columns.forEach { definition ->
            connection.prepare("ALTER TABLE track ADD COLUMN $definition").use { statement ->
                statement.step()
            }
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "ALTER TABLE track ADD COLUMN sourceStorageId INTEGER",
            "ALTER TABLE track ADD COLUMN sourcePath TEXT",
            "ALTER TABLE playlist ADD COLUMN coverStorageId INTEGER",
            "ALTER TABLE playlist ADD COLUMN coverPath TEXT",
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }
    }
}

private val trackV2Columns = listOf(
    "artist TEXT",
    "lyricist TEXT",
    "conductor TEXT",
    "copyright TEXT",
    "publisher TEXT",
    "originalReleaseDate TEXT",
    "bpm REAL",
    "musicalKey TEXT",
    "isrc TEXT",
    "musicBrainzRecordingId TEXT",
    "musicBrainzTrackId TEXT",
    "musicBrainzReleaseId TEXT",
    "musicBrainzReleaseGroupId TEXT",
    "musicBrainzArtistId TEXT",
    "musicBrainzReleaseArtistId TEXT",
    "musicBrainzWorkId TEXT",
    "replayGainTrackGain REAL",
    "replayGainTrackPeak REAL",
    "replayGainAlbumGain REAL",
    "replayGainAlbumPeak REAL",
)
