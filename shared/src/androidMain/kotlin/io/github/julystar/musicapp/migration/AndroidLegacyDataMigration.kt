package io.github.julystar.musicapp.migration

import io.github.julystar.musicapp.platform.appContext
import java.io.File

internal object AndroidLegacyDataMigration {
    @Volatile
    private var completed = false

    @Synchronized
    fun ensureMigrated() {
        if (completed) return
        val context = appContext.applicationContext
        LegacyPaths.FILE_MAPPINGS.take(3).forEach { (sourceName, targetName) ->
            migrateFile(
                source = context.getDatabasePath(sourceName),
                target = context.getDatabasePath(targetName),
            )
        }
        migrateFile(
            source = context.filesDir.resolve(LegacyPaths.PREFERENCES_FILE),
            target = context.filesDir.resolve(AppIdentifiers.PREFERENCES_FILE),
        )
        completed = true
    }

    private fun migrateFile(source: File, target: File) {
        if (!source.isFile || target.exists()) return
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) return
        source.copyTo(target)
        check(source.length() == target.length()) {
            "Legacy data migration size mismatch for ${source.name}"
        }
        check(source.delete()) {
            "Migrated ${source.name}, but could not remove the legacy copy"
        }
    }
}
