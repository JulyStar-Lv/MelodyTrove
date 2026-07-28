package io.github.julystar.musicapp.migration

import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

internal object IosLegacyDataMigration {
    private var completed = false

    fun ensureMigrated(dataDirectory: String) {
        if (completed) return
        val fileManager = NSFileManager.defaultManager
        LegacyPaths.FILE_MAPPINGS.forEach { (sourceName, targetName) ->
            val sourcePath = "$dataDirectory/$sourceName"
            val targetPath = "$dataDirectory/$targetName"
            if (!fileManager.fileExistsAtPath(sourcePath) || fileManager.fileExistsAtPath(targetPath)) {
                return@forEach
            }
            val moved = fileManager.moveItemAtURL(
                srcURL = NSURL.fileURLWithPath(sourcePath),
                toURL = NSURL.fileURLWithPath(targetPath),
                error = null,
            )
            if (!moved) {
                check(
                    fileManager.copyItemAtURL(
                        srcURL = NSURL.fileURLWithPath(sourcePath),
                        toURL = NSURL.fileURLWithPath(targetPath),
                        error = null,
                    )
                ) { "Unable to migrate legacy iOS data file $sourceName" }
            }
        }
        completed = true
    }
}
