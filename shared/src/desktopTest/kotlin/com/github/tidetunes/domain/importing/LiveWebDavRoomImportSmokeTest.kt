package com.github.tidetunes.domain.importing

import androidx.room.Room
import androidx.room.Transactor
import androidx.room.useWriterConnection
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.core.data.ToastRepositoryImpl
import com.github.tidetunes.core.data.security.CredentialStore
import com.github.tidetunes.core.domain.model.StoredCredential
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.SourceAccountEntity
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TideTunesDatabaseConstructor
import com.github.tidetunes.singleton.Bridge
import com.github.tidetunes.source.storage.MetadataRepository
import com.github.tidetunes.source.storage.RemoteScannerRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import uniffi.tidetunes_backend.StorageEntry
import uniffi.tidetunes_backend.StorageId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LiveWebDavRoomImportSmokeTest {
    @Test
    fun importsConfiguredWebDavMusicIntoRoom() = runBlocking {
        assumeTrue(
            "Set -D$LIVE_WEBDAV_ENABLED_PROPERTY=true to run the live WebDAV import smoke test.",
            java.lang.Boolean.getBoolean(LIVE_WEBDAV_ENABLED_PROPERTY),
        )

        val storageId = 42L
        val tempDir = File(
            System.getProperty("java.io.tmpdir"),
            "tidetunes-live-webdav-${System.nanoTime()}",
        ).apply { mkdirs() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val database = buildDatabase()
        val bridge = Bridge(
            appDocumentDir = tempDir.absolutePath,
            appCacheDir = tempDir.absolutePath,
            toastRepository = ToastRepositoryImpl(scope),
        )
        val credentialStore = InMemoryCredentialStore()

        try {
            bridge.initialize()
            database.sourceAccountDao().upsert(
                SourceAccountEntity(
                    id = storageId,
                    providerType = ProviderTypes.WebDav,
                    displayName = "Live WebDAV",
                    endpoint = LIVE_WEBDAV_URL,
                    externalAccountId = null,
                    credentialRef = "live-webdav-smoke",
                    priority = 0,
                    enabled = true,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            )
            credentialStore.save(
                storageId,
                StoredCredential(
                    username = LIVE_WEBDAV_USERNAME,
                    secret = LIVE_WEBDAV_PASSWORD,
                    isAnonymous = false,
                ),
            )

            val storageRepository = StorageRepositoryImpl(
                bridge = bridge,
                scope = scope,
                sourceAccountDao = database.sourceAccountDao(),
                credentialStore = credentialStore,
            )
            val scannerRepository = RemoteScannerRepository(
                bridge = bridge,
                storageRepository = storageRepository,
            )
            val entries = scannerRepository.collectMusicEntries(
                storageId = storageId,
                path = "/",
                limit = LIVE_WEBDAV_IMPORT_LIMIT,
            )

            assertEquals(
                LIVE_WEBDAV_IMPORT_LIMIT,
                entries.size,
                "live WebDAV scan returned ${entries.size} music entries",
            )

            val coordinator = RemoteLibraryImportCoordinator(
                database = database,
                trackDao = database.trackDao(),
                metadataDao = database.metadataDao(),
                syncDao = database.syncDao(),
                metadataRepository = MetadataRepository(
                    bridge = bridge,
                    storageRepository = storageRepository,
                ),
                remoteScannerRepository = scannerRepository,
                storageRepository = storageRepository,
            )
            val result = coordinator.importCompleteSnapshot(
                RemoteLibraryImportRequest(
                    storageId = storageId,
                    selectedFolderRemoteId = null,
                    selectedFolderCanonicalPath = "/",
                    selectedFolderDisplayPath = "Live WebDAV",
                    entries = entries,
                    scanId = "live-webdav-smoke",
                    metadataConcurrency = 1u,
                    importBatchSize = entries.size.coerceAtLeast(1),
                ),
            )

            val root = assertNotNull(database.libraryRootDao().findByPath(storageId, "/"))
            val tracks = database.trackDao().page(limit = LIVE_WEBDAV_IMPORT_LIMIT, offset = 0)
            val sourceItemCount = database.sourceItemDao().countForLibraryRoot(root.id)

            println(
                "live_webdav_room_import " +
                    "scanned=${result.scannedCount} " +
                    "imported=${result.importedCount} " +
                    "failed=${result.failedCount} " +
                    "sourceItems=$sourceItemCount " +
                    "tracks=${tracks.size} " +
                    "first=${entries.first().name}",
            )

            assertEquals(entries.size.toLong(), result.scannedCount)
            assertEquals(entries.size.toLong(), sourceItemCount)
            assertEquals(0L, result.failedCount)
            assertEquals(LIVE_WEBDAV_IMPORT_LIMIT.toLong(), result.importedCount)
            assertEquals(result.importedCount.toInt(), tracks.size)
            tracks.forEach { track ->
                assertTrue(
                    database.trackSourceRefDao().countForTrack(track.id) > 0,
                    "track ${track.id} has no source ref",
                )
            }

            println(
                database.importedRoomTableReport(
                    storageId = storageId,
                    libraryRootId = root.id,
                    trackIds = tracks.map { it.id },
                )
            )
        } finally {
            bridge.destroy()
            database.close()
            scope.cancel()
            tempDir.deleteRecursively()
        }
    }

    private fun buildDatabase(): TideTunesDatabase {
        return Room.inMemoryDatabaseBuilder<TideTunesDatabase> {
            TideTunesDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }
}

private const val LIVE_WEBDAV_ENABLED_PROPERTY = "tidetunes.liveWebdav.enabled"
private const val LIVE_WEBDAV_URL = "http://192.168.50.100:5244/dav/我的音乐/"
private const val LIVE_WEBDAV_USERNAME = "Tulip"
private const val LIVE_WEBDAV_PASSWORD = "Tulip240308"
private const val LIVE_WEBDAV_IMPORT_LIMIT = 10
private const val LIVE_WEBDAV_SCAN_BATCH_SIZE = 25
private const val MAX_TABLE_CELL_CHARS = 160

private suspend fun RemoteScannerRepository.collectMusicEntries(
    storageId: Long,
    path: String,
    limit: Int,
): List<StorageEntry> {
    val entries = mutableListOf<StorageEntry>()
    val seenPaths = mutableSetOf<String>()
    val session = startMusicFolderScan(StorageId(storageId), path)
    try {
        while (entries.size < limit) {
            val batch = session.nextBatch(LIVE_WEBDAV_SCAN_BATCH_SIZE.toUInt())
            if (batch.cancelled) {
                error("live WebDAV scan was cancelled")
            }
            batch.entries
                .asSequence()
                .filter { entry -> !entry.isDir }
                .filter { entry -> seenPaths.add(entry.path) }
                .take(limit - entries.size)
                .forEach(entries::add)
            if (batch.done) break
        }
    } finally {
        session.cancel()
        session.close()
    }
    return entries
}

private suspend fun TideTunesDatabase.importedRoomTableReport(
    storageId: Long,
    libraryRootId: Long,
    trackIds: List<Long>,
): String {
    return useWriterConnection { connection ->
        val builder = StringBuilder()
        val tableRows = mutableListOf<Pair<String, SqlTableRows>>()
        importedTableQueries(
            storageId = storageId,
            libraryRootId = libraryRootId,
            trackIds = trackIds,
        ).forEach { query ->
            val rows = connection.tableRows(query.sql)
            if (rows.rows.isNotEmpty()) {
                tableRows += query.table to rows
            }
        }
        builder.appendLine("live_webdav_room_import_database_tables")
        builder.appendMarkdownTable(
            title = "room_table_row_counts",
            headers = listOf("table", "rows"),
            rows = tableRows.map { (table, rows) -> listOf(table, rows.rows.size.toString()) },
        )
        tableRows.forEach { (table, rows) ->
            builder.appendMarkdownTable(
                title = table,
                headers = rows.headers,
                rows = rows.rows,
            )
        }
        builder.toString()
    }
}

private fun StringBuilder.appendMarkdownTable(
    title: String,
    headers: List<String>,
    rows: List<List<String>>,
) {
    appendLine()
    appendLine("### $title")
    appendLine("| ${headers.joinToString(" | ") { it.tableCell() }} |")
    appendLine("| ${headers.joinToString(" | ") { "---" }} |")
    rows.forEach { row ->
        appendLine("| ${row.joinToString(" | ") { it.tableCell() }} |")
    }
}

private suspend fun Transactor.tableRows(sql: String): SqlTableRows {
    return usePrepared(sql) { statement ->
        val headers = statement.getColumnNames()
        val rows = mutableListOf<List<String>>()
        while (statement.step()) {
            rows += headers.indices.map(statement::cellValue)
        }
        SqlTableRows(headers = headers, rows = rows)
    }
}

private fun SQLiteStatement.cellValue(index: Int): String {
    return when (getColumnType(index)) {
        SQLITE_DATA_NULL -> "NULL"
        SQLITE_DATA_INTEGER -> getLong(index).toString()
        SQLITE_DATA_FLOAT -> getDouble(index).toString()
        SQLITE_DATA_TEXT -> getText(index)
        SQLITE_DATA_BLOB -> "<blob:${getBlob(index).size}>"
        else -> getText(index)
    }.limitCell()
}

private fun String.limitCell(): String {
    val compact = replace("\r", "\\r").replace("\n", "\\n")
    return if (compact.length <= MAX_TABLE_CELL_CHARS) {
        compact
    } else {
        compact.take(MAX_TABLE_CELL_CHARS) + "..."
    }
}

private fun String.tableCell(): String = replace("|", "\\|")

private fun List<Long>.sqlIds(): String = if (isEmpty()) "-1" else joinToString(",")

private data class SqlTableRows(
    val headers: List<String>,
    val rows: List<List<String>>,
)

private data class TableReportQuery(
    val table: String,
    val sql: String,
)

private fun importedTableQueries(
    storageId: Long,
    libraryRootId: Long,
    trackIds: List<Long>,
): List<TableReportQuery> {
    val trackIdSql = trackIds.sqlIds()
    return listOf(
        TableReportQuery(
            table = "source_account",
            sql = "SELECT * FROM source_account WHERE id = $storageId ORDER BY id",
        ),
        TableReportQuery(
            table = "library_root",
            sql = "SELECT * FROM library_root WHERE id = $libraryRootId ORDER BY id",
        ),
        TableReportQuery(
            table = "source_item",
            sql = "SELECT * FROM source_item WHERE libraryRootId = $libraryRootId ORDER BY id",
        ),
        TableReportQuery(
            table = "track",
            sql = "SELECT * FROM track WHERE id IN ($trackIdSql) ORDER BY title COLLATE NOCASE",
        ),
        TableReportQuery(
            table = "track_source_ref",
            sql = "SELECT * FROM track_source_ref WHERE trackId IN ($trackIdSql) ORDER BY trackId, sourceItemId",
        ),
        TableReportQuery(
            table = "album",
            sql = """
                SELECT * FROM album
                WHERE id IN (
                    SELECT albumId FROM track
                    WHERE id IN ($trackIdSql) AND albumId IS NOT NULL
                )
                ORDER BY id
            """.trimIndent(),
        ),
        TableReportQuery(
            table = "artist",
            sql = """
                SELECT * FROM artist
                WHERE id IN (
                    SELECT artistId FROM track_artist WHERE trackId IN ($trackIdSql)
                    UNION
                    SELECT aa.artistId
                    FROM album_artist aa
                    JOIN track t ON t.albumId = aa.albumId
                    WHERE t.id IN ($trackIdSql)
                )
                ORDER BY id
            """.trimIndent(),
        ),
        TableReportQuery(
            table = "genre",
            sql = """
                SELECT * FROM genre
                WHERE id IN (
                    SELECT genreId FROM track_genre WHERE trackId IN ($trackIdSql)
                )
                ORDER BY id
            """.trimIndent(),
        ),
        TableReportQuery(
            table = "track_artist",
            sql = "SELECT * FROM track_artist WHERE trackId IN ($trackIdSql) ORDER BY trackId, position",
        ),
        TableReportQuery(
            table = "album_artist",
            sql = """
                SELECT * FROM album_artist
                WHERE albumId IN (
                    SELECT albumId FROM track
                    WHERE id IN ($trackIdSql) AND albumId IS NOT NULL
                )
                ORDER BY albumId, position
            """.trimIndent(),
        ),
        TableReportQuery(
            table = "track_genre",
            sql = "SELECT * FROM track_genre WHERE trackId IN ($trackIdSql) ORDER BY trackId, genreId",
        ),
        TableReportQuery(
            table = "artwork",
            sql = """
                SELECT * FROM artwork
                WHERE trackId IN ($trackIdSql)
                   OR albumId IN (
                       SELECT albumId FROM track
                       WHERE id IN ($trackIdSql) AND albumId IS NOT NULL
                   )
                ORDER BY id
            """.trimIndent(),
        ),
        TableReportQuery(
            table = "lyrics",
            sql = "SELECT * FROM lyrics WHERE trackId IN ($trackIdSql) ORDER BY trackId, id",
        ),
        TableReportQuery(
            table = "raw_metadata",
            sql = "SELECT * FROM raw_metadata WHERE trackId IN ($trackIdSql) ORDER BY trackId, tagKey, id",
        ),
        TableReportQuery(
            table = "import_job",
            sql = "SELECT * FROM import_job WHERE libraryRootId = $libraryRootId ORDER BY updatedAt DESC",
        ),
        TableReportQuery(
            table = "source_sync_cursor",
            sql = "SELECT * FROM source_sync_cursor WHERE libraryRootId = $libraryRootId ORDER BY id",
        ),
        TableReportQuery(
            table = "source_error",
            sql = "SELECT * FROM source_error WHERE libraryRootId = $libraryRootId ORDER BY id",
        ),
        TableReportQuery(
            table = "track_fts",
            sql = "SELECT rowid, * FROM track_fts WHERE rowid IN ($trackIdSql) ORDER BY rowid",
        ),
    )
}

private class InMemoryCredentialStore : CredentialStore {
    private val credentials = mutableMapOf<Long, StoredCredential>()

    override suspend fun load(storageId: Long): StoredCredential? = credentials[storageId]

    override suspend fun save(storageId: Long, credential: StoredCredential) {
        credentials[storageId] = credential
    }

    override suspend fun delete(storageId: Long) {
        credentials.remove(storageId)
    }
}
