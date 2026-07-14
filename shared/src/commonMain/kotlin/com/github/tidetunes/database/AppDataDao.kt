package com.github.tidetunes.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AppDataDao {
    @Query("DELETE FROM plugin_config")
    suspend fun deletePluginConfigs()

    @Query("DELETE FROM plugin")
    suspend fun deletePlugins()

    @Query("DELETE FROM playlist_track")
    suspend fun deletePlaylistTracks()

    @Query("DELETE FROM playlist")
    suspend fun deletePlaylists()

    @Query("DELETE FROM raw_metadata")
    suspend fun deleteRawMetadata()

    @Query("DELETE FROM lyrics")
    suspend fun deleteLyrics()

    @Query("DELETE FROM artwork")
    suspend fun deleteArtwork()

    @Query("DELETE FROM track_genre")
    suspend fun deleteTrackGenres()

    @Query("DELETE FROM album_artist")
    suspend fun deleteAlbumArtists()

    @Query("DELETE FROM track_artist")
    suspend fun deleteTrackArtists()

    @Query("DELETE FROM track_source_ref")
    suspend fun deleteTrackSourceRefs()

    @Query("DELETE FROM track")
    suspend fun deleteTracks()

    @Query("DELETE FROM genre")
    suspend fun deleteGenres()

    @Query("DELETE FROM artist")
    suspend fun deleteArtists()

    @Query("DELETE FROM album")
    suspend fun deleteAlbums()

    @Query("DELETE FROM download_task")
    suspend fun deleteDownloadTasks()

    @Query("DELETE FROM source_error")
    suspend fun deleteSourceErrors()

    @Query("DELETE FROM source_sync_cursor")
    suspend fun deleteSourceSyncCursors()

    @Query("DELETE FROM import_job")
    suspend fun deleteImportJobs()

    @Query("DELETE FROM source_item_property")
    suspend fun deleteSourceItemProperties()

    @Query("DELETE FROM source_item")
    suspend fun deleteSourceItems()

    @Query("DELETE FROM library_root")
    suspend fun deleteLibraryRoots()

    @Query("DELETE FROM source_account")
    suspend fun deleteSourceAccounts()

    @Query("INSERT INTO track_fts(track_fts) VALUES('rebuild')")
    suspend fun rebuildTrackFts()
}
