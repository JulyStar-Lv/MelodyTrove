package com.github.tidetunes.feature.sources.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.OneDriveDriveInfo
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.core.domain.repository.ToastRepository
import com.github.tidetunes.service.librarysync.domain.LibrarySyncController
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.source.api.ImportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class Validated(
    val addrEmpty: Boolean = false,
    val aliasEmpty: Boolean = false,
    val usernameEmpty: Boolean = false,
    val passwordEmpty: Boolean = false,
) {
    fun valid(): Boolean {
        return !addrEmpty && !aliasEmpty && !usernameEmpty && !passwordEmpty
    }
}

private data class EditorInputs(
    val draft: SourceEditorDraft,
    val title: String,
    val musicCount: ULong,
    val validated: Validated,
    val removeModalOpen: Boolean,
    val oneDriveDrives: List<OneDriveDriveInfo> = emptyList(),
    val oneDriveDrivesLoading: Boolean = false,
)

class EditStorageVM constructor(
    private val storageRepository: StorageRepository,
    private val toastRepository: ToastRepository,
    private val importRepository: ImportRepository,
    private val librarySyncController: LibrarySyncController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _events = Channel<SourceEditorEvent>(Channel.BUFFERED)
    private val _title = MutableStateFlow("")
    private val _musicCount = MutableStateFlow(0uL)
    private val _draft = MutableStateFlow(defaultSourceEditorDraft())
    private var _draftBackups = HashMap<SourceEditorType, SourceEditorDraft>()
    private var _editorAccountId: String? = null

    private val _validated = MutableStateFlow(Validated())
    private val _removeModalOpen = MutableStateFlow(false)
    private val _testResult = MutableStateFlow(SourceConnectionTestStatus.None)
    private var _testJob: Job? = null
    private val _oneDriveDrives = MutableStateFlow<List<OneDriveDriveInfo>>(emptyList())
    private val _oneDriveDrivesLoading = MutableStateFlow(false)
    private var _oneDriveDriveJob: Job? = null

    val events = _events.receiveAsFlow()
    val state = combine(
        _draft,
        _title,
        _musicCount,
        _validated,
        _removeModalOpen,
    ) { draft, title, musicCount, validated, removeModalOpen ->
        EditorInputs(draft, title, musicCount, validated, removeModalOpen)
    }.combine(_oneDriveDrives) { inputs, oneDriveDrives ->
        inputs.copy(oneDriveDrives = oneDriveDrives)
    }.combine(_oneDriveDrivesLoading) { inputs, oneDriveDrivesLoading ->
        inputs.copy(oneDriveDrivesLoading = oneDriveDrivesLoading)
    }.combine(_testResult) { inputs, testResult ->
        sourceEditorState(
            draft = inputs.draft,
            title = inputs.title,
            musicCount = inputs.musicCount,
            validation = inputs.validated.toSourceEditorValidation(),
            removeDialogOpen = inputs.removeModalOpen,
            testResult = testResult,
            oneDriveDrives = inputs.oneDriveDrives,
            oneDriveDrivesLoading = inputs.oneDriveDrivesLoading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = sourceEditorState(
            draft = defaultSourceEditorDraft(),
            title = "",
            musicCount = 0u,
            validation = SourceEditorValidation(),
            removeDialogOpen = false,
            testResult = SourceConnectionTestStatus.None,
        ),
    )

    init {
        viewModelScope.launch {
            storageRepository.oauthRefreshToken.collect { refreshToken ->
                updateDraft { draft ->
                    if (draft.storageType == SourceEditorType.OneDrive) {
                        draft.copy(secret = refreshToken)
                    } else {
                        draft
                    }
                }
                if (refreshToken.isNotBlank()) {
                    loadOneDriveDrives(refreshToken)
                }
            }
        }

        _draft.value = defaultSourceEditorDraft()
        _title.value = ""
        _musicCount.value = 0u

        val id: Long? = savedStateHandle["id"]
        if (id != null && id >= 0) {
            viewModelScope.launch {
                val editorState = storageRepository.loadEditorState(id) ?: return@launch
                _editorAccountId = editorState.accountId.value
                _draft.value = editorState.draft
                _title.value = editorState.title
                _musicCount.value = editorState.musicCount

                storageRepository.loadCredentialByAccountId(editorState.accountId)?.let { credential ->
                    updateDraft { current ->
                        current.copy(
                            username = credential.username,
                            secret = credential.secret,
                            isAnonymous = credential.isAnonymous,
                        )
                    }
                    if (editorState.isOneDrive) {
                        loadOneDriveDrives(credential.secret)
                    }
                }
            }
        }
    }

    private fun test() {
        resetTestResult()
        if (!validate()) {
            return
        }
        _testResult.value = SourceConnectionTestStatus.Testing

        _testJob = viewModelScope.launch {
            _testResult.value = storageRepository.testSource(_draft.value)

            delay(5000)
            resetTestResult()
        }
    }

    fun onAction(action: SourceEditorAction) {
        when (action) {
            SourceEditorAction.NavigateBack -> sendEvent(SourceEditorEvent.NavigateBack)
            SourceEditorAction.TestConnection -> test()
            SourceEditorAction.Save -> saveAndNavigateBack()
            SourceEditorAction.OpenRemoveDialog -> openRemoveModal()
            SourceEditorAction.CloseRemoveDialog -> closeRemoveModal()
            SourceEditorAction.ConfirmRemove -> removeAndNavigateBack()
            SourceEditorAction.ImportLibraryFolder -> prepareImportAndNavigate()
            is SourceEditorAction.ChangeType -> changeType(action.storageType)
            is SourceEditorAction.WebDavAnonymousChanged -> updateDraft { draft ->
                draft.copy(isAnonymous = action.isAnonymous)
            }
            is SourceEditorAction.WebDavAliasChanged -> updateDraft { draft ->
                draft.copy(alias = action.value)
            }
            is SourceEditorAction.WebDavAddressChanged -> updateDraft { draft ->
                draft.copy(address = action.value)
            }
            is SourceEditorAction.WebDavUsernameChanged -> updateDraft { draft ->
                draft.copy(username = action.value)
            }
            is SourceEditorAction.WebDavPasswordChanged -> updateDraft { draft ->
                draft.copy(secret = action.value)
            }
            is SourceEditorAction.OneDriveAliasChanged -> updateDraft { draft ->
                draft.copy(alias = action.value)
            }
            SourceEditorAction.ConnectOneDrive -> connectOneDrive()
            SourceEditorAction.DisconnectOneDrive -> disconnectOneDrive()
            is SourceEditorAction.SelectOneDriveDrive -> selectOneDriveDrive(action.driveId)
        }
    }

    private fun openRemoveModal() {
        _removeModalOpen.value = true
    }

    private fun closeRemoveModal() {
        _removeModalOpen.value = false
    }

    private fun updateDraft(block: (draft: SourceEditorDraft) -> SourceEditorDraft) {
        _draft.value = block(_draft.value)
    }

    private fun changeType(storageType: SourceEditorType) {
        _draftBackups[_draft.value.storageType] = _draft.value

        val backup = _draftBackups[storageType]
        if (backup != null) {
            _draft.value = backup
        } else {
            val newDraft = SourceEditorDraft(
                id = _draft.value.id,
                address = "",
                alias = _draft.value.alias,
                username = "",
                secret = "",
                isAnonymous = false,
                storageType = storageType,
            )
            _draft.value = newDraft
        }
        _validated.value = Validated()
    }

    private fun validate(): Boolean {
        val draft = _draft.value
        _validated.value = Validated(
            addrEmpty = draft.address.isBlank(),
            aliasEmpty = if (draft.storageType == SourceEditorType.WebDav) {
                false
            } else {
                draft.alias.isBlank()
            },
            usernameEmpty = if (draft.storageType == SourceEditorType.WebDav) {
                !draft.isAnonymous && draft.username.isBlank()
            } else {
                false
            },
            passwordEmpty = if (draft.storageType == SourceEditorType.WebDav) {
                !draft.isAnonymous && draft.secret.isBlank()
            } else {
                draft.secret.isBlank()
            },
        )
        return _validated.value.valid()
    }

    private fun remove() {
        val accountId = _editorAccountId ?: return
        viewModelScope.launch {
            storageRepository.removeByAccountId(SourceAccountId(accountId))
        }
    }

    private fun saveAndNavigateBack() {
        viewModelScope.launch {
            if (finish()) {
                _events.send(SourceEditorEvent.NavigateBack)
            }
        }
    }

    private fun removeAndNavigateBack() {
        closeRemoveModal()
        remove()
        sendEvent(SourceEditorEvent.NavigateBack)
    }

    private fun prepareImportAndNavigate() {
        prepareImportLibraryFolder()
        sendEvent(SourceEditorEvent.OpenLibraryFolderImport)
    }

    private fun connectOneDrive() {
        viewModelScope.launch {
            val result = runCatching {
                startOneDriveOAuth()
            }
            result.onSuccess { authorizationUrl ->
                _events.send(SourceEditorEvent.OpenOneDriveOAuth(authorizationUrl))
            }.onFailure { error ->
                if (error is CancellationException) {
                    throw error
                }
                toastRepository.emitToast(
                    "Unable to start OneDrive sign-in: ${error.message ?: "unknown error"}"
                )
            }
        }
    }

    private fun prepareImportLibraryFolder() {
        importRepository.prepareCurrentDirectory { selection ->
            viewModelScope.launch {
                toastRepository.emitToast("Importing library folder...")
                val result = runCatching {
                    librarySyncController.syncFolder(
                        LibrarySyncRequest(
                            accountId = selection.accountId,
                            selectedFolderRemoteId = selection.remoteId,
                            selectedFolderCanonicalPath = selection.path,
                            selectedFolderDisplayPath = selection.path,
                        )
                    )
                }
                result.onSuccess { value ->
                    toastRepository.emitToast(
                        "Library import completed: ${value.importedCount} imported, " +
                            "${value.skippedCount} skipped, ${value.failedCount} failed"
                    )
                    storageRepository.reload()
                }.onFailure { error ->
                    if (error is CancellationException) {
                        toastRepository.emitToast("Library import cancelled")
                    } else {
                        toastRepository.emitToast(
                            "Library import failed: ${error.message ?: "unknown error"}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun startOneDriveOAuth(): String = storageRepository.startOneDriveOAuth()

    private fun selectOneDriveDrive(driveId: String) {
        updateDraft { draft ->
            draft.copy(address = driveId)
        }
    }

    private fun disconnectOneDrive() {
        _oneDriveDriveJob?.cancel()
        _oneDriveDrives.value = emptyList()
        _oneDriveDrivesLoading.value = false
        updateDraft { draft ->
            draft.copy(
                address = "",
                secret = "",
            )
        }
    }

    private suspend fun finish(): Boolean {
        if (!validate()) {
            return false
        }

        storageRepository.upsertSource(_draft.value)
        return true
    }

    private fun resetTestResult() {
        _testJob?.cancel()
        _testJob = null
        _testResult.value = SourceConnectionTestStatus.None
    }

    private fun sendEvent(event: SourceEditorEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    private fun loadOneDriveDrives(refreshToken: String) {
        _oneDriveDriveJob?.cancel()
        _oneDriveDriveJob = viewModelScope.launch {
            _oneDriveDrivesLoading.value = true
            try {
                val result = storageRepository.listOneDriveDriveInfos(refreshToken)
                _oneDriveDrives.value = result.drives
                if (result.refreshedToken != refreshToken) {
                    updateDraft { draft ->
                        draft.copy(secret = result.refreshedToken)
                    }
                    val accountId = _editorAccountId
                    if (accountId != null) {
                        storageRepository.updateOneDriveRefreshTokenByAccountId(
                            SourceAccountId(accountId),
                            result.refreshedToken,
                        )
                    }
                }
                val selected = _draft.value.address
                if (result.drives.isNotEmpty() && result.drives.none { it.id == selected }) {
                    selectOneDriveDrive(result.drives.first().id)
                }
            } catch (error: Exception) {
                _oneDriveDrives.value = emptyList()
                toastRepository.emitToast(
                    "Unable to list OneDrive drives: ${error.message ?: "unknown error"}"
                )
            } finally {
                _oneDriveDrivesLoading.value = false
            }
        }
    }
}

private fun Validated.toSourceEditorValidation(): SourceEditorValidation {
    return SourceEditorValidation(
        addressEmpty = addrEmpty,
        aliasEmpty = aliasEmpty,
        usernameEmpty = usernameEmpty,
        passwordEmpty = passwordEmpty,
    )
}
