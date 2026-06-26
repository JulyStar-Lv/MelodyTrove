package com.github.tidetune.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun databaseBuilder(): RoomDatabase.Builder<TideTuneDatabase> {
    val directory = File(System.getProperty("user.home"), ".tidetune")
    directory.mkdirs()
    return Room.databaseBuilder<TideTuneDatabase>(
        name = File(directory, "tidetune.db").absolutePath,
    )
}
