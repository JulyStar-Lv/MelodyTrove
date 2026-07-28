package io.github.julystar.musicapp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.julystar.musicapp.migration.AppIdentifiers
import io.github.julystar.musicapp.platform.getAppDataDirectory
import java.io.File

actual fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val directory = File(getAppDataDirectory()).apply { mkdirs() }
    return Room.databaseBuilder<AppDatabase>(
        name = File(directory, AppIdentifiers.DATABASE_FILE).absolutePath,
    )
}
