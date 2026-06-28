package com.github.tidetunes.database

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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            """
            CREATE TABLE IF NOT EXISTS download_task (
                id TEXT NOT NULL PRIMARY KEY,
                sourceId TEXT NOT NULL,
                mediaType TEXT NOT NULL,
                remoteId TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT,
                album TEXT,
                durationMs INTEGER,
                status TEXT NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                totalBytes INTEGER,
                localPath TEXT,
                mimeType TEXT,
                errorMessage TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_download_task_status ON download_task(status)",
            "CREATE INDEX IF NOT EXISTS index_download_task_updatedAt ON download_task(updatedAt)",
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_download_task_sourceId_mediaType_remoteId
            ON download_task(sourceId, mediaType, remoteId)
            """.trimIndent(),
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "ALTER TABLE track ADD COLUMN lastPlayedAt INTEGER",
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }
    }
}
