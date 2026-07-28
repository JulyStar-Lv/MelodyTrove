package io.github.julystar.musicapp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.julystar.musicapp.migration.AppIdentifiers
import io.github.julystar.musicapp.migration.IosLegacyDataMigration
import io.github.julystar.musicapp.platform.getAppDataDirectory

actual fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dataDirectory = getAppDataDirectory()
    IosLegacyDataMigration.ensureMigrated(dataDirectory)
    return Room.databaseBuilder<AppDatabase>(
        name = "$dataDirectory/${AppIdentifiers.DATABASE_FILE}",
    )
}
