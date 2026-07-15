package com.github.tidetunes.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        SourceAccountEntity::class,
        LibraryRootEntity::class,
        SourceItemEntity::class,
        SourceItemPropertyEntity::class,
        TrackEntity::class,
        TrackSourceRefEntity::class,
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
        SourceSyncCursorEntity::class,
        SourceErrorEntity::class,
        DownloadTaskEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        TrackFts::class,
        PluginEntity::class,
        PluginConfigEntity::class,
    ],
    version = TIDE_TUNES_DATABASE_VERSION,
    exportSchema = true,
)
@ConstructedBy(TideTunesDatabaseConstructor::class)
abstract class TideTunesDatabase : RoomDatabase() {
    abstract fun sourceAccountDao(): SourceAccountDao
    abstract fun libraryRootDao(): LibraryRootDao
    abstract fun sourceItemDao(): SourceItemDao
    abstract fun trackSourceRefDao(): TrackSourceRefDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun metadataDao(): MetadataDao
    abstract fun syncDao(): SyncDao
    abstract fun sourceSyncCursorDao(): SourceSyncCursorDao
    abstract fun sourceErrorDao(): SourceErrorDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun trackFtsDao(): TrackFtsDao
    abstract fun pluginDao(): PluginDao
    abstract fun appDataDao(): AppDataDao
}

const val TIDE_TUNES_DATABASE_VERSION = 14

@Suppress("KotlinNoActualForExpect")
expect object TideTunesDatabaseConstructor : RoomDatabaseConstructor<TideTunesDatabase> {
    override fun initialize(): TideTunesDatabase
}

expect fun databaseBuilder(): RoomDatabase.Builder<TideTunesDatabase>

fun buildDatabase(): TideTunesDatabase = databaseBuilder()
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.Default)
    .addMigrations(MIGRATION_1_2)
    .addMigrations(MIGRATION_2_3)
    .addMigrations(MIGRATION_3_4)
    .addMigrations(MIGRATION_4_5)
    .addMigrations(MIGRATION_5_6)
    .addMigrations(MIGRATION_6_7)
    .addMigrations(MIGRATION_7_8)
    .addMigrations(MIGRATION_8_9)
    .addMigrations(MIGRATION_9_10)
    .addMigrations(MIGRATION_10_11)
    .addMigrations(MIGRATION_11_12)
    .addMigrations(MIGRATION_12_13)
    .addMigrations(MIGRATION_13_14)
    .build()
