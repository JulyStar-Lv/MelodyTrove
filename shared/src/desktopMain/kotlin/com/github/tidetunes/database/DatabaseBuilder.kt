package com.github.tidetunes.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun databaseBuilder(): RoomDatabase.Builder<TideTunesDatabase> {
    val directory = File(System.getProperty("user.home"), ".tidetunes")
    directory.mkdirs()
    return Room.databaseBuilder<TideTunesDatabase>(
        name = File(directory, "tidetunes.db").absolutePath,
    )
}
