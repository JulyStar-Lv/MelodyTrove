package com.github.tidetune.database

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
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
    ],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(TideTuneDatabaseConstructor::class)
abstract class TideTuneDatabase : RoomDatabase() {
    abstract fun storageDao(): StorageDao
    abstract fun selectedFolderDao(): SelectedFolderDao
    abstract fun remoteFileDao(): RemoteFileDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun metadataDao(): MetadataDao
    abstract fun syncDao(): SyncDao
}

@Suppress("KotlinNoActualForExpect")
expect object TideTuneDatabaseConstructor : RoomDatabaseConstructor<TideTuneDatabase> {
    override fun initialize(): TideTuneDatabase
}

expect fun databaseBuilder(): RoomDatabase.Builder<TideTuneDatabase>

fun buildDatabase(): TideTuneDatabase {
    return databaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .build()
}
