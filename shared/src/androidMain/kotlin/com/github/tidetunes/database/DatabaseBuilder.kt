package com.github.tidetunes.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.tidetunes.platform.appContext

actual fun databaseBuilder(): RoomDatabase.Builder<TideTunesDatabase> {
    val context = appContext.applicationContext
    val path = context.getDatabasePath("tidetunes.db").absolutePath
    return Room.databaseBuilder<TideTunesDatabase>(
        context = context,
        name = path,
    )
}
