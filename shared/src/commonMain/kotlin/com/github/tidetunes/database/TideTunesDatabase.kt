package com.github.tidetunes.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        StorageEntity::class,
        SelectedFolderEntity::class,
        RemoteFileEntity::class,
        TrackEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        TrackArtistCrossRef::class,
        AlbumArtistCrossRef::class,
        GenreEntity::class,
        TrackGenreCrossRef::class,
        ArtworkEntity::class,
        LyricsEntity::class,
        RawMetadataEntity::class,
        ImportJobEntity::class,
        SyncCursorEntity::class,
        DownloadTaskEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
    ],
    version = 5,
    exportSchema = true,
)
@ConstructedBy(TideTunesDatabaseConstructor::class)
abstract class TideTunesDatabase : RoomDatabase() {
    abstract fun storageDao(): StorageDao
    abstract fun selectedFolderDao(): SelectedFolderDao
    abstract fun remoteFileDao(): RemoteFileDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun metadataDao(): MetadataDao
    abstract fun syncDao(): SyncDao
    abstract fun downloadTaskDao(): DownloadTaskDao


}

@Suppress("KotlinNoActualForExpect")
expect object TideTunesDatabaseConstructor : RoomDatabaseConstructor<TideTunesDatabase> {
    override fun initialize(): TideTunesDatabase
}

expect fun databaseBuilder(): RoomDatabase.Builder<TideTunesDatabase>

fun buildDatabase(): TideTunesDatabase {
    return databaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .addMigrations(MIGRATION_4_5)
        .build()
}

