package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DiagnosticsExportResult
import com.github.tidetunes.core.domain.model.DiagnosticsReport
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.LibraryRebuildState
import com.github.tidetunes.core.domain.model.LocalMusicDirectory
import com.github.tidetunes.core.domain.model.LyricTextAlignment
import com.github.tidetunes.core.domain.model.MetadataRefreshTarget
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.OneDriveDriveListResult
import com.github.tidetunes.core.domain.model.SettingsCapabilities
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorStorageState
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.StorageUsage
import com.github.tidetunes.core.domain.model.StoredCredential
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.repository.DiagnosticsService
import com.github.tidetunes.core.domain.repository.AppDataClearService
import com.github.tidetunes.core.domain.repository.LibraryMaintenanceService
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.SourceSettingsRepository
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.core.domain.repository.ToastRepository
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.service.librarysync.domain.LibrarySyncResult
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshController
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshRequest
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshResult
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.ImportRepository
import com.github.tidetunes.source.api.SourceDirectorySelection
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.source.api.SourceNodeType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsVMTest {

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads settings modifies values reports failures and gates unsupported capabilities`() = runTest {
        val repository = FakeSettingsRepository(
            AppSettings.Default.copy(
                themeMode = AppThemeMode.Light,
                dynamicColorEnabled = false,
                scanSubdirectories = false,
            )
        )
        val environment = TestEnvironment(settingsRepository = repository)
        withStartedViewModel(environment) { viewModel ->
            assertEquals(AppThemeMode.Light, viewModel.state.value.settings.themeMode)
            assertFalse(viewModel.state.value.capabilities.dynamicColorSupported)

            viewModel.onAction(SettingsAction.SetThemeMode(AppThemeMode.Dark))
            advanceUntilIdle()
            assertEquals(AppThemeMode.Dark, repository.values.value.themeMode)

            viewModel.onAction(SettingsAction.SetDynamicColorEnabled(true))
            viewModel.onAction(SettingsAction.SetGaplessPlaybackEnabled(true))
            advanceUntilIdle()
            assertFalse(repository.values.value.dynamicColorEnabled)
            assertFalse(repository.values.value.gaplessPlaybackEnabled)

            repository.failThemeUpdates = true
            viewModel.onAction(SettingsAction.SetThemeMode(AppThemeMode.System))
            advanceUntilIdle()
            assertEquals(AppThemeMode.Dark, repository.values.value.themeMode)
            assertTrue(environment.toast.messages.last().contains("write failed"))

            viewModel.onAction(SettingsAction.SetAutoScanMode(AutoScanMode.OnStartup))
            viewModel.onAction(SettingsAction.SetLyricTextAlignment(LyricTextAlignment.Center))
            viewModel.onAction(SettingsAction.SetLyricPrimaryFontScalePercent(125))
            advanceUntilIdle()
            assertEquals(AutoScanMode.OnStartup, repository.values.value.autoScanMode)
            assertEquals(LyricTextAlignment.Center, repository.values.value.lyrics.textAlignment)
            assertEquals(125, repository.values.value.lyrics.primaryFontScalePercent)
            assertFalse(repository.values.value.scanSubdirectories)
        }
    }

    @Test
    fun `dangerous actions require confirmation before clearing cache or rebuilding library`() = runTest {
        val environment = TestEnvironment()
        withStartedViewModel(environment) { viewModel ->
            viewModel.onAction(SettingsAction.RequestClearAllCaches)
            advanceUntilIdle()
            assertIs<SettingsConfirmation.ClearAllCaches>(viewModel.state.value.pendingConfirmation)
            assertEquals(0, environment.storageUsage.clearAllCalls)

            viewModel.onAction(SettingsAction.ConfirmPendingAction)
            advanceUntilIdle()
            assertNull(viewModel.state.value.pendingConfirmation)
            assertEquals(1, environment.storageUsage.clearAllCalls)

            viewModel.onAction(SettingsAction.RequestRebuildLibrary)
            advanceUntilIdle()
            assertIs<SettingsConfirmation.RebuildLibrary>(viewModel.state.value.pendingConfirmation)
            assertEquals(0, environment.maintenance.rebuildCalls)

            viewModel.onAction(SettingsAction.ConfirmPendingAction)
            advanceUntilIdle()
            assertEquals(1, environment.maintenance.rebuildCalls)
        }
    }

    @Test
    fun `clearing all app data requires confirmation and invokes the wipe service`() = runTest {
        val environment = TestEnvironment()
        withStartedViewModel(environment) { viewModel ->
            viewModel.onAction(SettingsAction.RequestClearAllData)
            advanceUntilIdle()

            assertIs<SettingsConfirmation.ClearAllData>(viewModel.state.value.pendingConfirmation)
            assertEquals(0, environment.appDataClear.clearCalls)

            viewModel.onAction(SettingsAction.ConfirmPendingAction)
            advanceUntilIdle()

            assertNull(viewModel.state.value.pendingConfirmation)
            assertEquals(1, environment.appDataClear.clearCalls)
        }
    }

    @Test
    fun `cache changes use the repository limit and enforce the same value`() = runTest {
        val environment = TestEnvironment()
        withStartedViewModel(environment) { viewModel ->
            val requestedLimit = 2_147_483_648L
            viewModel.onAction(SettingsAction.SetAudioCacheLimitBytes(requestedLimit))
            advanceUntilIdle()

            assertEquals(requestedLimit, environment.settingsRepository.values.value.audioCacheLimitBytes)
            assertEquals(requestedLimit, environment.storageUsage.lastEnforcedAudioLimit)
            assertEquals(
                environment.settingsRepository.values.value.imageCacheLimitBytes,
                environment.storageUsage.lastEnforcedImageLimit,
            )
        }
    }

    @Test
    fun `shows failures for the selected scan`() = runTest {
        val failure = LibrarySyncFailure("metadata", "unreadable file", 7L)
        val sync = FakeLibrarySyncController().apply {
            failuresByTask["scan-1"] = MutableStateFlow(listOf(failure))
        }
        val environment = TestEnvironment(librarySyncController = sync)
        withStartedViewModel(environment) { viewModel ->
            viewModel.onAction(SettingsAction.OpenScanFailures("scan-1"))
            advanceUntilIdle()

            assertEquals("scan-1", viewModel.state.value.failureDialogTaskId)
            assertEquals(listOf(failure), viewModel.state.value.failureDetails)
        }
    }

    @Test
    fun `editing WebDAV loads account data but never restores its password`() = runTest {
        val accountId = storageSourceAccountId(42L)
        val storage = FakeStorageRepository().apply {
            accounts.value = listOf(
                StorageAccountInfo(
                    accountId = accountId,
                    sourceId = BuiltInSourceIds.WebDav,
                    isLocal = false,
                    isOneDrive = false,
                    title = "Home DAV",
                    subtitle = "https://dav.example.test",
                    musicCount = 12,
                    rootPath = "/Music",
                )
            )
            editorState = SourceEditorStorageState(
                accountId = accountId,
                draft = SourceEditorDraft(
                    id = 42L,
                    address = "https://dav.example.test",
                    alias = "Home DAV",
                    username = "stored-user",
                    secret = "must-not-reach-ui",
                ),
                title = "Home DAV",
                musicCount = 12u,
                isOneDrive = false,
            )
            credential = StoredCredential("stored-user", "must-not-reach-ui", false)
        }
        val environment = TestEnvironment(storageRepository = storage)
        withStartedViewModel(environment) { viewModel ->
            viewModel.onAction(SettingsAction.OpenEditWebDavDialog(accountId))
            advanceUntilIdle()

            val dialog = viewModel.state.value.webDavDialog ?: error("WebDAV editor was not opened")
            assertEquals("stored-user", dialog.username)
            assertFalse(dialog.toString().contains("must-not-reach-ui"))
            assertFalse(dialog.toString().contains("password", ignoreCase = true))

            viewModel.onAction(SettingsAction.SaveWebDavAccount(""))
            advanceUntilIdle()

            assertEquals("must-not-reach-ui", storage.upsertedDraft?.secret)
            assertFalse(viewModel.state.value.toString().contains("must-not-reach-ui"))

            viewModel.onAction(SettingsAction.OpenEditWebDavDialog(accountId))
            advanceUntilIdle()
            viewModel.onAction(SettingsAction.SaveWebDavAccount("replacement-secret"))
            advanceUntilIdle()

            assertEquals("replacement-secret", storage.upsertedDraft?.secret)
            assertFalse(viewModel.state.value.toString().contains("replacement-secret"))
        }
    }

    @Test
    fun `home state uses real supported source counts and excludes unfinished providers`() = runTest {
        val storage = FakeStorageRepository().apply {
            accounts.value = listOf(
                sourceAccount(1L, BuiltInSourceIds.Local, "Local", 4),
                sourceAccount(
                    id = 2L,
                    sourceId = BuiltInSourceIds.WebDav,
                    title = "DAV",
                    count = 6,
                    lastScanAtEpochMs = 1_725_000_000_000L,
                    lastScanStatus = "SYNCED",
                ),
                sourceAccount(3L, BuiltInSourceIds.OneDrive, "OneDrive", 99),
            )
        }
        val environment = TestEnvironment(storageRepository = storage)
        withStartedViewModel(environment) { viewModel ->
            assertEquals(listOf("Local", "DAV"), viewModel.state.value.sourceAccounts.map { it.title })
            assertEquals(2, viewModel.state.value.enabledSourceCount)
            assertEquals(10, viewModel.state.value.trackCount)
            val webDav = viewModel.state.value.sourceAccounts.single { it.title == "DAV" }
            assertEquals(1_725_000_000_000L, webDav.lastScanAtEpochMs)
            assertEquals("SYNCED", webDav.lastScanStatus)
        }
    }

    @Test
    fun `WebDAV scan uses latest metadata mode while local scan remains full`() = runTest {
        val localAccountId = storageSourceAccountId(1L)
        val webDavAccountId = storageSourceAccountId(2L)
        val settings = FakeSettingsRepository(
            AppSettings.Default.copy(webDavMetadataScanMode = MetadataScanMode.Standard)
        )
        val storage = FakeStorageRepository().apply {
            accounts.value = listOf(
                sourceAccount(1L, BuiltInSourceIds.Local, "Local", 1),
                sourceAccount(2L, BuiltInSourceIds.WebDav, "DAV", 1),
            )
        }
        val sync = FakeLibrarySyncController()
        val environment = TestEnvironment(
            settingsRepository = settings,
            storageRepository = storage,
            librarySyncController = sync,
        ).apply {
            sourceSettings.localDirectories.value = listOf(
                LocalMusicDirectory(
                    id = "local-root",
                    accountId = localAccountId,
                    displayName = "Music",
                    path = "/Music",
                    lastScannedAtEpochMs = null,
                )
            )
        }

        withStartedViewModel(environment) { viewModel ->
            viewModel.onAction(SettingsAction.SetWebDavMetadataScanMode(MetadataScanMode.Fast))
            advanceUntilIdle()
            assertEquals(MetadataScanMode.Fast, settings.values.value.webDavMetadataScanMode)

            viewModel.onAction(SettingsAction.ScanSourceAccount(webDavAccountId))
            viewModel.onAction(SettingsAction.ScanLocalMusic)
            advanceUntilIdle()

            assertEquals(
                MetadataScanMode.Fast,
                sync.requests.single { it.accountId == webDavAccountId }.metadataScanMode,
            )
            assertEquals(
                MetadataScanMode.Full,
                sync.requests.single { it.accountId == localAccountId }.metadataScanMode,
            )
        }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.withStartedViewModel(
        environment: TestEnvironment,
        block: suspend (SettingsVM) -> Unit,
    ) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel = environment.createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect()
        }
        advanceUntilIdle()
        block(viewModel)
    }
}

private class TestEnvironment(
    val settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    val storageRepository: FakeStorageRepository = FakeStorageRepository(),
    val librarySyncController: FakeLibrarySyncController = FakeLibrarySyncController(),
) {
    val sourceSettings = FakeSourceSettingsRepository()
    val storageUsage = FakeStorageUsageRepository()
    val maintenance = FakeLibraryMaintenanceService()
    val appDataClear = FakeAppDataClearService()
    val toast = FakeToastRepository()

    fun createViewModel() = SettingsVM(
        settingsRepository = settingsRepository,
        sourceSettingsRepository = sourceSettings,
        storageRepository = storageRepository,
        storageUsageRepository = storageUsage,
        diagnosticsService = FakeDiagnosticsService(),
        libraryMaintenanceService = maintenance,
        appDataClearService = appDataClear,
        toastRepository = toast,
        importRepository = FakeImportRepository(),
        librarySyncController = librarySyncController,
        metadataRefreshController = FakeMetadataRefreshController(),
        capabilities = SettingsCapabilities(),
        textProvider = FakeSettingsTextProvider(),
    )
}

private class FakeSettingsTextProvider : SettingsTextProvider {
    override suspend fun get(
        resource: org.jetbrains.compose.resources.StringResource,
        vararg formatArgs: Any,
    ): String = formatArgs.joinToString(" ")
}

private class FakeSettingsRepository(initial: AppSettings = AppSettings.Default) : SettingsRepository {
    val values = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = values
    var failThemeUpdates = false

    private fun update(block: (AppSettings) -> AppSettings) {
        values.value = block(values.value)
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        if (failThemeUpdates) error("write failed")
        update { it.copy(themeMode = mode) }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) = update { it.copy(dynamicColorEnabled = enabled) }
    override suspend fun setLanguageMode(mode: AppLanguageMode) = update { it.copy(languageMode = mode) }
    override suspend fun setAudioFocusMode(mode: AudioFocusMode) = update { it.copy(audioFocusMode = mode) }
    override suspend fun setPauseOnDisconnect(enabled: Boolean) = update { it.copy(pauseOnDisconnect = enabled) }
    override suspend fun setGaplessPlaybackEnabled(enabled: Boolean) = update { it.copy(gaplessPlaybackEnabled = enabled) }
    override suspend fun setRetryPlaybackOnFailure(enabled: Boolean) = update { it.copy(retryPlaybackOnFailure = enabled) }
    override suspend fun setResumePlaybackAfterNetworkRecovery(enabled: Boolean) = update { it.copy(resumePlaybackAfterNetworkRecovery = enabled) }
    override suspend fun setKeepScreenOnInPlayer(enabled: Boolean) = update { it.copy(keepScreenOnInPlayer = enabled) }
    override suspend fun setLyricTextAlignment(alignment: LyricTextAlignment) =
        update { it.copy(lyrics = it.lyrics.copy(textAlignment = alignment)) }
    override suspend fun setLyricPrimaryFontScalePercent(value: Int) =
        update { it.copy(lyrics = it.lyrics.copy(primaryFontScalePercent = value)) }
    override suspend fun setLyricPrimaryFontSizeSp(value: Int) =
        update { it.copy(lyrics = it.lyrics.copy(primaryFontSizeSp = value)) }
    override suspend fun setLyricSecondaryFontScalePercent(value: Int) =
        update { it.copy(lyrics = it.lyrics.copy(secondaryFontScalePercent = value)) }
    override suspend fun setLyricSecondaryFontSizeSp(value: Int) =
        update { it.copy(lyrics = it.lyrics.copy(secondaryFontSizeSp = value)) }
    override suspend fun setLyricTranslationVisible(visible: Boolean) =
        update { it.copy(lyrics = it.lyrics.copy(showTranslation = visible)) }
    override suspend fun setLyricWordLiftEnabled(enabled: Boolean) =
        update { it.copy(lyrics = it.lyrics.copy(wordLiftEnabled = enabled)) }
    override suspend fun setLyricBlurEffectEnabled(enabled: Boolean) =
        update { it.copy(lyrics = it.lyrics.copy(blurEffectEnabled = enabled)) }
    override suspend fun setLyricPerspectiveEffectEnabled(enabled: Boolean) =
        update { it.copy(lyrics = it.lyrics.copy(perspectiveEffectEnabled = enabled)) }
    override suspend fun setLyricPerspectiveAngleDegrees(value: Int) =
        update { it.copy(lyrics = it.lyrics.copy(perspectiveAngleDegrees = value)) }
    override suspend fun setLyricTapToSeekEnabled(enabled: Boolean) =
        update { it.copy(lyrics = it.lyrics.copy(tapToSeekEnabled = enabled)) }
    override suspend fun setAutoScanMode(mode: AutoScanMode) = update { it.copy(autoScanMode = mode) }
    override suspend fun setBackgroundScanEnabled(enabled: Boolean) = update { it.copy(backgroundScanEnabled = enabled) }
    override suspend fun setScanOnlyOnUnmeteredNetwork(enabled: Boolean) = update { it.copy(scanOnlyOnUnmeteredNetwork = enabled) }
    override suspend fun setScanSubdirectories(enabled: Boolean) = update { it.copy(scanSubdirectories = enabled) }
    override suspend fun setWebDavMetadataScanMode(mode: MetadataScanMode) = update { it.copy(webDavMetadataScanMode = mode) }
    override suspend fun setMinimumAudioDurationMs(value: Long) = update { it.copy(minimumAudioDurationMs = value) }
    override suspend fun setMissingFilePolicy(policy: MissingFilePolicy) = update { it.copy(missingFilePolicy = policy) }
    override suspend fun setDuplicateTrackPolicy(policy: DuplicateTrackPolicy) = update { it.copy(duplicateTrackPolicy = policy) }
    override suspend fun setAllowMeteredStreaming(enabled: Boolean) = update { it.copy(allowMeteredStreaming = enabled) }
    override suspend fun setBackgroundSyncOnlyOnUnmeteredNetwork(enabled: Boolean) = update { it.copy(backgroundSyncOnlyOnUnmeteredNetwork = enabled) }
    override suspend fun setNetworkRetryCount(value: Int) = update { it.copy(networkRetryCount = value) }
    override suspend fun setConnectionTimeoutSeconds(value: Int) = update { it.copy(connectionTimeoutSeconds = value) }
    override suspend fun setAudioPreloadBytes(bytes: Long) = update { it.copy(audioPreloadBytes = bytes) }
    override suspend fun setAudioCacheLimitBytes(bytes: Long) = update { it.copy(audioCacheLimitBytes = bytes) }
    override suspend fun setImageCacheLimitBytes(bytes: Long) = update { it.copy(imageCacheLimitBytes = bytes) }
    override suspend fun resetToDefaults() { values.value = AppSettings.Default }
}

private class FakeSourceSettingsRepository : SourceSettingsRepository {
    override val localDirectories = MutableStateFlow<List<LocalMusicDirectory>>(emptyList())
    override suspend fun setAccountEnabled(accountId: SourceAccountId, enabled: Boolean) = Unit
    override suspend fun removeLocalDirectory(id: String) = Unit
}

private class FakeStorageRepository : StorageRepository {
    val accounts = MutableStateFlow<List<StorageAccountInfo>>(emptyList())
    override val storageAccounts: StateFlow<List<StorageAccountInfo>> = accounts
    override val onRemoveStorageEvent: SharedFlow<Unit> = MutableSharedFlow()
    override val oauthRefreshToken: StateFlow<String> = MutableStateFlow("")
    var editorState: SourceEditorStorageState? = null
    var credential: StoredCredential? = null
    var upsertedDraft: SourceEditorDraft? = null

    override suspend fun reload() = Unit
    override suspend fun startOneDriveOAuth(): String = ""
    override suspend fun upsertSource(draft: SourceEditorDraft): SourceAccountId {
        upsertedDraft = draft
        return storageSourceAccountId(draft.id ?: 1L)
    }
    override suspend fun loadEditorState(id: Long): SourceEditorStorageState? = editorState
    override suspend fun testSource(draft: SourceEditorDraft) = SourceConnectionTestStatus.Success
    override suspend fun listOneDriveDriveInfos(refreshToken: String) = OneDriveDriveListResult(emptyList(), refreshToken)
    override suspend fun updateOneDriveRefreshTokenByAccountId(accountId: SourceAccountId, refreshToken: String) = Unit
    override fun findStorageAccountByAccountId(accountId: SourceAccountId) = accounts.value.firstOrNull { it.accountId == accountId }
    override suspend fun loadCredentialByAccountId(accountId: SourceAccountId): StoredCredential? = credential
    override suspend fun setAccountRootPath(accountId: SourceAccountId, rootPath: String) = Unit
    override suspend fun removeByAccountId(accountId: SourceAccountId) = Unit
}

private class FakeStorageUsageRepository : StorageUsageRepository {
    var clearAllCalls = 0
    var lastEnforcedAudioLimit: Long? = null
    var lastEnforcedImageLimit: Long? = null

    override suspend fun loadUsage() = StorageUsage(totalBytes = 0)
    override suspend fun clearAudioCache() = Unit
    override suspend fun clearImageCache() = Unit
    override suspend fun clearAllCaches() { clearAllCalls += 1 }
    override suspend fun clearAllStoredFiles() = Unit
    override suspend fun enforceCacheLimits(audioLimitBytes: Long, imageLimitBytes: Long) {
        lastEnforcedAudioLimit = audioLimitBytes
        lastEnforcedImageLimit = imageLimitBytes
    }
}

private class FakeLibraryMaintenanceService : LibraryMaintenanceService {
    override val rebuildState = MutableStateFlow(LibraryRebuildState())
    var rebuildCalls = 0
    override suspend fun rebuildLibrary() { rebuildCalls += 1 }
}

private class FakeAppDataClearService : AppDataClearService {
    var clearCalls = 0
    override suspend fun clearAllData() { clearCalls += 1 }
}

private class FakeToastRepository : ToastRepository {
    val messages = mutableListOf<String>()
    override val toast: SharedFlow<String> = MutableSharedFlow()
    override val toastRes: SharedFlow<Int> = MutableSharedFlow()
    override fun emitToast(msg: String) { messages += msg }
    override fun emitToastRes(resId: Int) = Unit
}

private class FakeDiagnosticsService : DiagnosticsService {
    override suspend fun collectDiagnostics(): DiagnosticsReport = error("not used")
    override suspend fun exportDiagnostics(): DiagnosticsExportResult = DiagnosticsExportResult.Success("diagnostics.txt")
}

private class FakeImportRepository : ImportRepository {
    override val allowTypes = MutableStateFlow<List<SourceNodeType>>(emptyList())
    override val selectionMode = MutableStateFlow(
        com.github.tidetunes.core.domain.model.ImportSelectionMode.Entries
    )
    override fun prepare(types: List<SourceNodeType>, block: (List<SourceNodeSelection>) -> Unit) = Unit
    override fun prepareCurrentDirectory(block: (SourceDirectorySelection) -> Unit) = Unit
    override fun onFinish(entries: List<SourceNodeSelection>) = Unit
    override fun onFinishCurrentDirectory(selection: SourceDirectorySelection) = Unit
}

private class FakeLibrarySyncController : LibrarySyncController {
    override val recentTasks = MutableStateFlow<List<LibrarySyncTask>>(emptyList())
    val failuresByTask = mutableMapOf<String, MutableStateFlow<List<LibrarySyncFailure>>>()
    val requests = mutableListOf<LibrarySyncRequest>()
    override fun observeFailures(taskId: String): Flow<List<LibrarySyncFailure>> =
        failuresByTask.getOrPut(taskId) { MutableStateFlow(emptyList()) }
    override suspend fun syncFolder(request: LibrarySyncRequest): LibrarySyncResult {
        requests += request
        return LibrarySyncResult(
            scanId = "scan-${requests.size}",
            selectedFolderId = requests.size.toLong(),
            scannedCount = 0,
            changedCount = 0,
            skippedCount = 0,
            importedCount = 0,
            failedCount = 0,
        )
    }
    override suspend fun pause(scanId: String) = false
    override suspend fun cancel(scanId: String) = false
    override suspend fun cancelAll() = Unit
    override suspend fun recoverInterruptedTasks() = 0
    override suspend fun resume(scanId: String): LibrarySyncResult? = null
    override suspend fun retry(scanId: String): LibrarySyncResult? = null
}

private class FakeMetadataRefreshController : MetadataRefreshController {
    override suspend fun refresh(request: MetadataRefreshRequest) = MetadataRefreshResult(
        requestedCount = 0,
        refreshedCount = 0,
        failedCount = 0,
        metadataRequestCount = 0,
        metadataFetchedBytes = 0,
        metadataElapsedMs = 0,
        artworkCachedBytes = 0,
    )
}

private fun sourceAccount(
    id: Long,
    sourceId: com.github.tidetunes.core.domain.model.SourceId,
    title: String,
    count: Long,
    lastScanAtEpochMs: Long? = null,
    lastScanStatus: String? = null,
) = StorageAccountInfo(
    accountId = storageSourceAccountId(id),
    sourceId = sourceId,
    isLocal = sourceId == BuiltInSourceIds.Local,
    isOneDrive = sourceId == BuiltInSourceIds.OneDrive,
    title = title,
    subtitle = title,
    musicCount = count,
    lastScanAtEpochMs = lastScanAtEpochMs,
    lastScanStatus = lastScanStatus,
)
