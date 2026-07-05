package com.github.tidetunes.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.LocalMusicDirectory
import com.github.tidetunes.core.domain.model.MAX_AUDIO_CACHE_LIMIT_BYTES
import com.github.tidetunes.core.domain.model.SettingsCapabilities
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.domain.model.SourceEditorDraft
import com.github.tidetunes.core.domain.model.SourceEditorType
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.StorageUsage
import com.github.tidetunes.core.domain.model.normalizeAudioCacheLimitBytes
import com.github.tidetunes.core.domain.model.storageSourceAccountId
import com.github.tidetunes.core.domain.model.toStorageRouteIdOrNull
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.domain.repository.SourceSettingsRepository
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.core.domain.repository.StorageUsageRepository
import com.github.tidetunes.core.domain.repository.ToastRepository
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.service.librarysync.domain.LibrarySyncScanRules
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.ImportRepository
import com.github.tidetunes.source.api.SourceDirectorySelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsVM(
    private val settingsRepository: SettingsRepository,
    private val sourceSettingsRepository: SourceSettingsRepository,
    private val storageRepository: StorageRepository,
    private val storageUsageRepository: StorageUsageRepository,
    private val toastRepository: ToastRepository,
    private val importRepository: ImportRepository,
    private val librarySyncController: LibrarySyncController,
    private val capabilities: SettingsCapabilities,
) : ViewModel() {
    private val storageUsage = MutableStateFlow(StorageUsage.Unknown)
    private val storageRefreshing = MutableStateFlow(false)
    private val pendingConfirmation = MutableStateFlow<SettingsConfirmation?>(null)
    private val customCacheLimitDialogOpen = MutableStateFlow(false)
    private val customCacheLimitInputMb = MutableStateFlow("")
    private val sourceOperationInProgress = MutableStateFlow(false)
    private val webDavDialog = MutableStateFlow<WebDavAccountDialogState?>(null)
    private val webDavConnectionTestStatus = MutableStateFlow(SourceConnectionTestStatus.None)
    private val webDavConnectionTestMessage = MutableStateFlow<String?>(null)
    private val failureDialogTaskId = MutableStateFlow<String?>(null)
    private val events = Channel<SettingsEvent>(Channel.BUFFERED)

    val eventFlow = events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val failureDetails = failureDialogTaskId
        .flatMapLatest { taskId ->
            if (taskId == null) {
                flowOf(emptyList())
            } else {
                librarySyncController.observeFailures(taskId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        storageUsage,
        storageRefreshing,
        pendingConfirmation,
        customCacheLimitDialogOpen,
        customCacheLimitInputMb,
        sourceSettingsRepository.localDirectories,
        storageRepository.storageAccounts,
        librarySyncController.recentTasks,
        sourceOperationInProgress,
        webDavDialog,
        webDavConnectionTestStatus,
        webDavConnectionTestMessage,
        failureDialogTaskId,
        failureDetails,
    ) { values ->
        val settings = values[0] as AppSettings
        val localDirectories = values[6] as List<LocalMusicDirectory>
        val storageAccounts = values[7] as List<StorageAccountInfo>
        val scanTasks = values[8] as List<LibrarySyncTask>
        val webDavAccounts = storageAccounts
            .filter { account -> account.sourceId == BuiltInSourceIds.WebDav }
            .map { account ->
                WebDavAccountSettingsItem(
                    accountId = account.accountId,
                    title = account.title.ifBlank { "WebDAV" },
                    subtitle = account.subtitle,
                    rootPath = settings.webDavRootPaths[account.accountId.value] ?: "/",
                )
            }

        SettingsUiState(
            settings = settings,
            capabilities = capabilities,
            storageUsage = values[1] as StorageUsage,
            storageRefreshing = values[2] as Boolean,
            pendingConfirmation = values[3] as SettingsConfirmation?,
            customCacheLimitDialogOpen = values[4] as Boolean,
            customCacheLimitInputMb = values[5] as String,
            localDirectories = localDirectories,
            webDavAccounts = webDavAccounts,
            scanTasks = scanTasks.filterRelevantToSettings(localDirectories, webDavAccounts),
            sourceOperationInProgress = values[9] as Boolean,
            webDavDialog = values[10] as WebDavAccountDialogState?,
            webDavConnectionTestStatus = values[11] as SourceConnectionTestStatus,
            webDavConnectionTestMessage = values[12] as String?,
            failureDialogTaskId = values[13] as String?,
            failureDetails = values[14] as List<LibrarySyncFailure>,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(capabilities = capabilities),
    )

    init {
        refreshStorageUsage()
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            sourceSettingsRepository.setLocalMusicEnabled(settings.localMusicEnabled)
            sourceSettingsRepository.setWebDavEnabled(settings.webDavEnabled)
            storageRepository.reload()
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> viewModelScope.launch {
                settingsRepository.setThemeMode(action.mode)
            }
            is SettingsAction.SetDynamicColorEnabled -> viewModelScope.launch {
                if (capabilities.dynamicColorSupported || !action.enabled) {
                    settingsRepository.setDynamicColorEnabled(action.enabled)
                }
            }
            is SettingsAction.SetLanguageMode -> viewModelScope.launch {
                settingsRepository.setLanguageMode(action.mode)
                toastRepository.emitToast("语言将在重启后生效")
            }
            is SettingsAction.SetPauseOnDisconnect -> viewModelScope.launch {
                settingsRepository.setPauseOnDisconnect(action.enabled)
            }
            is SettingsAction.SetAllowMixedPlayback -> viewModelScope.launch {
                settingsRepository.setAllowMixedPlayback(action.enabled)
            }
            is SettingsAction.SetKeepScreenOnInPlayer -> viewModelScope.launch {
                settingsRepository.setKeepScreenOnInPlayer(action.enabled)
            }
            is SettingsAction.SetLocalMusicEnabled -> setLocalMusicEnabled(action.enabled)
            is SettingsAction.SetLocalScanSubdirectories -> viewModelScope.launch {
                settingsRepository.setLocalScanSubdirectories(action.enabled)
            }
            is SettingsAction.SetIgnoreShortAudio -> viewModelScope.launch {
                settingsRepository.setIgnoreShortAudio(action.enabled)
            }
            SettingsAction.RequestAddLocalDirectory -> requestAddLocalDirectory()
            is SettingsAction.RequestRemoveLocalDirectory -> {
                pendingConfirmation.value = SettingsConfirmation.RemoveLocalDirectory(
                    id = action.id,
                    title = action.title,
                )
            }
            SettingsAction.ScanLocalMusic -> scanLocalMusic()
            is SettingsAction.SetWebDavEnabled -> setWebDavEnabled(action.enabled)
            is SettingsAction.SetWebDavScanSubdirectories -> viewModelScope.launch {
                settingsRepository.setWebDavScanSubdirectories(action.enabled)
            }
            SettingsAction.OpenAddWebDavDialog -> openAddWebDavDialog()
            is SettingsAction.OpenEditWebDavDialog -> openEditWebDavDialog(action.accountId)
            SettingsAction.DismissWebDavDialog -> dismissWebDavDialog()
            is SettingsAction.SetWebDavDialogName -> updateWebDavDialog {
                it.copy(name = action.value)
            }
            is SettingsAction.SetWebDavDialogServerUrl -> updateWebDavDialog {
                it.copy(serverUrl = action.value)
            }
            is SettingsAction.SetWebDavDialogUsername -> updateWebDavDialog {
                it.copy(username = action.value)
            }
            is SettingsAction.SetWebDavDialogPassword -> updateWebDavDialog {
                it.copy(password = action.value)
            }
            is SettingsAction.SetWebDavDialogRootPath -> updateWebDavDialog {
                it.copy(rootPath = action.value)
            }
            SettingsAction.TestWebDavConnection -> testWebDavConnection()
            SettingsAction.SaveWebDavAccount -> saveWebDavAccount()
            is SettingsAction.RequestDeleteWebDavAccount -> {
                pendingConfirmation.value = SettingsConfirmation.DeleteWebDavAccount(
                    accountId = action.accountId,
                    title = action.title,
                )
            }
            is SettingsAction.ScanWebDavAccount -> scanWebDavAccount(action.accountId)
            is SettingsAction.CancelScan -> cancelScan(action.scanId)
            is SettingsAction.OpenScanFailures -> {
                failureDialogTaskId.value = action.scanId
            }
            SettingsAction.DismissScanFailures -> {
                failureDialogTaskId.value = null
            }
            is SettingsAction.SetAudioCacheLimitBytes -> viewModelScope.launch {
                settingsRepository.setAudioCacheLimitBytes(action.bytes)
            }
            is SettingsAction.SetCustomCacheLimitInput -> {
                customCacheLimitInputMb.value = action.value.filter { it.isDigit() }
            }
            SettingsAction.OpenCustomCacheLimitDialog -> {
                customCacheLimitInputMb.value = currentCacheLimitMbInput()
                customCacheLimitDialogOpen.value = true
            }
            SettingsAction.DismissCustomCacheLimitDialog -> {
                customCacheLimitDialogOpen.value = false
            }
            SettingsAction.ApplyCustomCacheLimit -> applyCustomCacheLimit()
            SettingsAction.RefreshStorageUsage -> refreshStorageUsage()
            SettingsAction.RequestClearAudio -> {
                pendingConfirmation.value = SettingsConfirmation.ClearAudio
            }
            SettingsAction.RequestClearImage -> {
                pendingConfirmation.value = SettingsConfirmation.ClearImage
            }
            SettingsAction.DismissConfirmation -> {
                pendingConfirmation.value = null
            }
            SettingsAction.ConfirmPendingAction -> confirmPendingAction()
        }
    }

    private fun setLocalMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalMusicEnabled(enabled)
            sourceSettingsRepository.setLocalMusicEnabled(enabled)
            storageRepository.reload()
        }
    }

    private fun setWebDavEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWebDavEnabled(enabled)
            sourceSettingsRepository.setWebDavEnabled(enabled)
            storageRepository.reload()
        }
    }

    private fun requestAddLocalDirectory() {
        importRepository.prepareCurrentDirectory { selection ->
            syncSelectedDirectory(selection)
        }
        viewModelScope.launch {
            events.send(SettingsEvent.OpenLibraryFolderImport)
        }
    }

    private fun syncSelectedDirectory(selection: SourceDirectorySelection) {
        viewModelScope.launch {
            syncFolder(
                request = LibrarySyncRequest(
                    accountId = selection.accountId,
                    selectedFolderRemoteId = selection.remoteId,
                    selectedFolderCanonicalPath = selection.path,
                    selectedFolderDisplayPath = selection.path,
                    scanRules = state.value.settings.localScanRules(),
                ),
                startMessage = "开始扫描音乐目录",
            )
        }
    }

    private fun scanLocalMusic() {
        val directories = state.value.localDirectories
        if (directories.isEmpty()) {
            viewModelScope.launch { toastRepository.emitToast("请先添加本地音乐目录") }
            return
        }
        directories.forEach { directory ->
            viewModelScope.launch {
                syncFolder(
                    request = LibrarySyncRequest(
                        accountId = directory.accountId,
                        selectedFolderRemoteId = null,
                        selectedFolderCanonicalPath = directory.path,
                        selectedFolderDisplayPath = directory.path,
                        scanRules = state.value.settings.localScanRules(),
                    ),
                    startMessage = "开始扫描 ${directory.displayName}",
                )
            }
        }
    }

    private fun openAddWebDavDialog() {
        webDavDialog.value = WebDavAccountDialogState()
        resetWebDavTest()
    }

    private fun openEditWebDavDialog(accountId: SourceAccountId) {
        viewModelScope.launch {
            val routeId = accountId.toStorageRouteIdOrNull() ?: return@launch
            val editorState = storageRepository.loadEditorState(routeId) ?: return@launch
            val credential = storageRepository.loadCredentialByAccountId(accountId)
            val rootPath = state.value.settings.webDavRootPaths[accountId.value] ?: "/"
            webDavDialog.value = WebDavAccountDialogState(
                accountId = accountId,
                name = editorState.draft.alias,
                serverUrl = editorState.draft.address,
                username = credential?.username.orEmpty(),
                password = "",
                rootPath = rootPath,
            )
            resetWebDavTest()
        }
    }

    private fun dismissWebDavDialog() {
        webDavDialog.value = null
        resetWebDavTest()
    }

    private fun updateWebDavDialog(block: (WebDavAccountDialogState) -> WebDavAccountDialogState) {
        val current = webDavDialog.value ?: return
        webDavDialog.value = block(current)
        resetWebDavTest()
    }

    private fun testWebDavConnection() {
        val dialog = webDavDialog.value ?: return
        viewModelScope.launch {
            val draft = dialog.toWebDavDraftOrNull() ?: return@launch
            webDavConnectionTestStatus.value = SourceConnectionTestStatus.Testing
            webDavConnectionTestMessage.value = "测试中"
            val result = runCatching { storageRepository.testSource(draft) }
            result.onSuccess { status ->
                webDavConnectionTestStatus.value = status
                webDavConnectionTestMessage.value = when (status) {
                    SourceConnectionTestStatus.Success -> "连接成功，扫描时会校验根目录 ${dialog.rootPath.normalizedRootPath()}"
                    SourceConnectionTestStatus.Error -> "连接失败，请检查地址和账号"
                    SourceConnectionTestStatus.Testing -> "测试中"
                    SourceConnectionTestStatus.None -> null
                }
            }.onFailure { error ->
                webDavConnectionTestStatus.value = SourceConnectionTestStatus.Error
                webDavConnectionTestMessage.value = "连接失败：${error.message ?: "未知错误"}"
            }
        }
    }

    private fun saveWebDavAccount() {
        val dialog = webDavDialog.value ?: return
        viewModelScope.launch {
            val draft = dialog.toWebDavDraftOrNull() ?: return@launch
            sourceOperationInProgress.value = true
            val result = runCatching {
                val accountId = storageRepository.upsertSource(draft)
                settingsRepository.setWebDavRootPath(
                    accountId = accountId,
                    rootPath = dialog.rootPath.normalizedRootPath(),
                )
                storageRepository.reload()
                accountId
            }
            sourceOperationInProgress.value = false
            result.onSuccess {
                webDavDialog.value = null
                resetWebDavTest()
                toastRepository.emitToast("已保存 WebDAV 账号")
            }.onFailure { error ->
                toastRepository.emitToast("保存失败：${error.message ?: "未知错误"}")
            }
        }
    }

    private fun scanWebDavAccount(accountId: SourceAccountId) {
        if (!state.value.settings.webDavEnabled) {
            viewModelScope.launch { toastRepository.emitToast("请先开启 WebDAV") }
            return
        }
        val account = state.value.webDavAccounts.firstOrNull { it.accountId == accountId }
        val rootPath = account?.rootPath?.normalizedRootPath() ?: "/"
        viewModelScope.launch {
            syncFolder(
                request = LibrarySyncRequest(
                    accountId = accountId,
                    selectedFolderRemoteId = null,
                    selectedFolderCanonicalPath = rootPath,
                    selectedFolderDisplayPath = rootPath,
                    scanRules = state.value.settings.webDavScanRules(),
                ),
                startMessage = "开始扫描 ${account?.title ?: "WebDAV"}",
            )
        }
    }

    private fun cancelScan(scanId: String) {
        viewModelScope.launch {
            val cancelled = librarySyncController.cancel(scanId)
            toastRepository.emitToast(if (cancelled) "已取消扫描" else "当前扫描无法取消")
        }
    }

    private suspend fun WebDavAccountDialogState.toWebDavDraftOrNull(): SourceEditorDraft? {
        val address = serverUrl.trim()
        if (address.isBlank()) {
            toastRepository.emitToast("请输入服务器 URL")
            return null
        }
        val usernameValue = username.trim()
        val typedPassword = password
        val previousCredential = accountId?.let { storageRepository.loadCredentialByAccountId(it) }
        val wantsAnonymous = usernameValue.isBlank() && typedPassword.isBlank()
        val secretValue = if (wantsAnonymous) {
            ""
        } else {
            typedPassword.ifBlank { previousCredential?.secret.orEmpty() }
        }
        if (usernameValue.isNotBlank() && secretValue.isBlank()) {
            toastRepository.emitToast("请输入密码，或清空用户名以匿名访问")
            return null
        }

        return SourceEditorDraft(
            id = accountId?.toStorageRouteIdOrNull(),
            address = address,
            alias = name.trim(),
            username = if (wantsAnonymous) "" else usernameValue,
            secret = secretValue,
            isAnonymous = wantsAnonymous,
            storageType = SourceEditorType.WebDav,
        )
    }

    private suspend fun syncFolder(
        request: LibrarySyncRequest,
        startMessage: String,
    ) {
        toastRepository.emitToast(startMessage)
        val result = runCatching { librarySyncController.syncFolder(request) }
        result.onSuccess { value ->
            toastRepository.emitToast(
                "扫描完成：总数 ${value.scannedCount}，导入 ${value.importedCount}，失败 ${value.failedCount}"
            )
            storageRepository.reload()
        }.onFailure { error ->
            if (error is CancellationException) throw error
            toastRepository.emitToast("扫描失败：${error.message ?: "未知错误"}")
        }
    }

    private fun applyCustomCacheLimit() {
        val maxMb = MAX_AUDIO_CACHE_LIMIT_BYTES / BYTES_PER_MB
        val inputMb = customCacheLimitInputMb.value.toLongOrNull() ?: 0L
        val normalizedMb = inputMb.coerceIn(0L, maxMb)
        customCacheLimitInputMb.value = normalizedMb.toString()
        customCacheLimitDialogOpen.value = false
        viewModelScope.launch {
            settingsRepository.setAudioCacheLimitBytes(
                normalizeAudioCacheLimitBytes(normalizedMb * BYTES_PER_MB)
            )
        }
    }

    private fun confirmPendingAction() {
        val action = pendingConfirmation.value ?: return
        pendingConfirmation.value = null
        viewModelScope.launch {
            when (action) {
                SettingsConfirmation.ClearAudio -> {
                    storageUsageRepository.clearAudioCache()
                    toastRepository.emitToast("已清理音频缓存")
                    refreshStorageUsage()
                }
                SettingsConfirmation.ClearImage -> {
                    storageUsageRepository.clearImageCache()
                    toastRepository.emitToast("已清理图片缓存")
                    refreshStorageUsage()
                }
                is SettingsConfirmation.RemoveLocalDirectory -> {
                    sourceSettingsRepository.removeLocalDirectory(action.id)
                    toastRepository.emitToast("已移除目录，不会删除本地文件")
                }
                is SettingsConfirmation.DeleteWebDavAccount -> {
                    storageRepository.removeByAccountId(action.accountId)
                    settingsRepository.removeWebDavRootPath(action.accountId)
                    storageRepository.reload()
                    webDavDialog.value = null
                    resetWebDavTest()
                    toastRepository.emitToast("已删除 WebDAV 账号，不会删除远程文件")
                }
            }
        }
    }

    private fun refreshStorageUsage() {
        viewModelScope.launch {
            storageRefreshing.value = true
            storageUsage.value = runCatching { storageUsageRepository.loadUsage() }
                .getOrElse { StorageUsage.Unknown }
            storageRefreshing.value = false
        }
    }

    private fun resetWebDavTest() {
        webDavConnectionTestStatus.value = SourceConnectionTestStatus.None
        webDavConnectionTestMessage.value = null
    }

    private fun currentCacheLimitMbInput(): String {
        val bytes = state.value.settings.audioCacheLimitBytes
        return (bytes / BYTES_PER_MB).toString()
    }
}

private fun List<LibrarySyncTask>.filterRelevantToSettings(
    localDirectories: List<LocalMusicDirectory>,
    webDavAccounts: List<WebDavAccountSettingsItem>,
): List<LibrarySyncTask> {
    val localAccountIds = localDirectories.map { it.accountId }.toSet() + storageSourceAccountId(LOCAL_STORAGE_ID)
    val webDavAccountIds = webDavAccounts.map { it.accountId }.toSet()
    return filter { task -> task.accountId in localAccountIds || task.accountId in webDavAccountIds }
}

private fun String.normalizedRootPath(): String {
    val trimmed = trim().ifBlank { "/" }
    return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
}

private fun AppSettings.localScanRules(): LibrarySyncScanRules {
    return LibrarySyncScanRules(
        scanSubdirectories = localScanSubdirectories,
        ignoreShortAudio = ignoreShortAudio,
    )
}

private fun AppSettings.webDavScanRules(): LibrarySyncScanRules {
    return LibrarySyncScanRules(
        scanSubdirectories = webDavScanSubdirectories,
        ignoreShortAudio = ignoreShortAudio,
    )
}

private const val BYTES_PER_MB = 1_048_576L
private const val LOCAL_STORAGE_ID = 1L
