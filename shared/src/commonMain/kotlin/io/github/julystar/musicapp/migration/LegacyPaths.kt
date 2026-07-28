package io.github.julystar.musicapp.migration

internal object LegacyPaths {
    const val DESKTOP_DATA_DIRECTORY = ".tidetunes"
    const val DATABASE_FILE = "tidetunes.db"
    const val DATABASE_WAL_FILE = "tidetunes.db-wal"
    const val DATABASE_SHM_FILE = "tidetunes.db-shm"
    const val PREFERENCES_FILE = "tidetunes.preferences_pb"

    val FILE_MAPPINGS = listOf(
        DATABASE_FILE to AppIdentifiers.DATABASE_FILE,
        DATABASE_WAL_FILE to "${AppIdentifiers.DATABASE_FILE}-wal",
        DATABASE_SHM_FILE to "${AppIdentifiers.DATABASE_FILE}-shm",
        PREFERENCES_FILE to AppIdentifiers.PREFERENCES_FILE,
    )

    val DATA_DIRECTORIES = listOf(
        "plugins",
        "downloads",
        "logs",
        "cache",
        "artwork",
        "lyrics",
        "backups",
        "diagnostics",
        "credentials",
    )
}
