package com.github.tidetunes.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.github.tidetunes.core.data.security.CredentialStore
import com.github.tidetunes.core.domain.repository.AppDataClearService
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.plugin.runtime.PluginRuntimeManager
import com.github.tidetunes.service.download.domain.DownloadController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.playback.domain.PlaybackController

class RoomAppDataClearService(
    private val playbackController: PlaybackController,
    private val librarySyncController: LibrarySyncController,
    private val downloadController: DownloadController,
    private val pluginRuntimeManager: PluginRuntimeManager,
    private val credentialStore: CredentialStore,
    private val database: TideTunesDatabase,
    private val dataStore: DataStore<Preferences>,
    private val settingsRepository: SettingsRepository,
    private val storageUsageRepository: StorageUsageRepository,
) : AppDataClearService {
    override suspend fun clearAllData() {
        playbackController.clearQueue()
        librarySyncController.cancelAll()
        downloadController.cancelAll()
        pluginRuntimeManager.closeAll()
        credentialStore.clear()
        database.clearAllAppData()
        settingsRepository.resetToDefaults()
        dataStore.edit { preferences -> preferences.clear() }
        storageUsageRepository.clearAllStoredFiles()
    }
}

internal suspend fun TideTunesDatabase.clearAllAppData() {
    useWriterConnection { connection ->
        connection.immediateTransaction {
            val dao = appDataDao()
            dao.deleteListeningHistory()
            dao.deletePluginConfigs()
            dao.deletePlugins()
            dao.deletePlaylistTracks()
            dao.deletePlaylists()
            dao.deleteRawMetadata()
            dao.deleteLyrics()
            dao.deleteArtwork()
            dao.deleteTrackGenres()
            dao.deleteAlbumArtists()
            dao.deleteTrackArtists()
            dao.deleteTrackSourceRefs()
            dao.deleteTracks()
            dao.rebuildTrackFts()
            dao.deleteGenres()
            dao.deleteArtists()
            dao.deleteAlbums()
            dao.deleteDownloadTasks()
            dao.deleteSourceErrors()
            dao.deleteSourceSyncCursors()
            dao.deleteImportJobs()
            dao.deleteSourceItemProperties()
            dao.deleteSourceItems()
            dao.deleteLibraryRoots()
            dao.deleteSourceAccounts()
        }
    }
}
