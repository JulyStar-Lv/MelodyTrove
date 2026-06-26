package com.github.tidetune.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.tidetune.platform.appContext

actual fun databaseBuilder(): RoomDatabase.Builder<TideTuneDatabase> {
    val context = appContext.applicationContext
    val path = context.getDatabasePath("tidetune.db").absolutePath
    return Room.databaseBuilder<TideTuneDatabase>(
        context = context,
        name = path,
    )
}
