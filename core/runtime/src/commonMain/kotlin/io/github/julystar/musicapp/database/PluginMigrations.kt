package io.github.julystar.musicapp.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "ALTER TABLE plugin ADD COLUMN allowManualLookup INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE plugin ADD COLUMN allowAutomaticLookup INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE plugin ADD COLUMN allowBatchLookup INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE plugin ADD COLUMN lastError TEXT",
            "ALTER TABLE plugin ADD COLUMN lastErrorAt INTEGER",
        ).forEach { sql ->
            connection.prepare(sql).use { statement -> statement.step() }
        }
    }
}
