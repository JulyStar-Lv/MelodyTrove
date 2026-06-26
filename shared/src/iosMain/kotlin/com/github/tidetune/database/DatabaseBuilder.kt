package com.github.tidetune.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.tidetune.platform.getAppDocumentDir

actual fun databaseBuilder(): RoomDatabase.Builder<TideTuneDatabase> {
    return Room.databaseBuilder<TideTuneDatabase>(
        name = "${getAppDocumentDir()}/tidetune.db",
    )
}
