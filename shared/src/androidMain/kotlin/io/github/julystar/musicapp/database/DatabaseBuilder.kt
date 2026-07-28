package io.github.julystar.musicapp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.julystar.musicapp.migration.AndroidLegacyDataMigration
import io.github.julystar.musicapp.migration.AppIdentifiers
import io.github.julystar.musicapp.platform.appContext

actual fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    AndroidLegacyDataMigration.ensureMigrated()
    val context = appContext.applicationContext
    val path = context.getDatabasePath(AppIdentifiers.DATABASE_FILE).absolutePath
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = path,
    )
}
