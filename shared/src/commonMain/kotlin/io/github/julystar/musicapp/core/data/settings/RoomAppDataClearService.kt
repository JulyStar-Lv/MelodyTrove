package io.github.julystar.musicapp.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.julystar.musicapp.core.data.security.CredentialStore
import io.github.julystar.musicapp.core.domain.repository.AppDataClearService
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import io.github.julystar.musicapp.core.domain.repository.StorageUsageRepository
import io.github.julystar.musicapp.database.AppDatabase
import io.github.julystar.musicapp.plugin.runtime.PluginRuntimeManager
import io.github.julystar.musicapp.service.download.domain.DownloadController
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncController
import io.github.julystar.musicapp.service.playback.domain.PlaybackController

class RoomAppDataClearService(
    private val playbackController: PlaybackController,
    private val librarySyncController: LibrarySyncController,
    private val downloadController: DownloadController,
    private val pluginRuntimeManager: PluginRuntimeManager,
    private val credentialStore: CredentialStore,
    private val database: AppDatabase,
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

internal suspend fun AppDatabase.clearAllAppData() {
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
