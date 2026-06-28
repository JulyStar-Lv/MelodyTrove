package com.github.tidetunes.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.tidetunes.platform.getAppDocumentDir

actual fun databaseBuilder(): RoomDatabase.Builder<TideTunesDatabase> {
    return Room.databaseBuilder<TideTunesDatabase>(
        name = "${getAppDocumentDir()}/tidetunes.db",
    )
}
