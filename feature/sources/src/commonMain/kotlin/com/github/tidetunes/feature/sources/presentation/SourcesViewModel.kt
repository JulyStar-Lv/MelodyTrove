package com.github.tidetunes.feature.sources.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.source.api.BuiltInSourceIds
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourcesViewModel(
    private val storageRepository: StorageRepository,
) : ViewModel() {
    private val _events = Channel<SourcesEvent>(Channel.BUFFERED)

    val events = _events.receiveAsFlow()
    val state = storageRepository.storageAccounts
        .map { accounts ->
            SourcesState(
                sources = accounts
                    .filter { account -> !account.isLocal }
                    .map { account -> account.toSourceAccountUi() }
                    .toPersistentList(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SourcesState(),
        )

    init {
        reload()
    }

    fun onAction(action: SourcesAction) {
        when (action) {
            SourcesAction.Refresh -> reload()
            SourcesAction.AddSource -> openNewSourceEditor()
            is SourcesAction.OpenSource -> openSourceEditor(action.id)
        }
    }

    private fun reload() {
        viewModelScope.launch {
            storageRepository.reload()
        }
    }

    private fun openNewSourceEditor() {
        viewModelScope.launch {
            _events.send(SourcesEvent.OpenNewSourceEditor)
        }
    }

    private fun openSourceEditor(id: SourceAccountId) {
        viewModelScope.launch {
            _events.send(SourcesEvent.OpenSourceEditor(id))
        }
    }
}

private fun StorageAccountInfo.toSourceAccountUi(): SourceAccountUi {
    return SourceAccountUi(
        id = accountId,
        title = title,
        subtitle = subtitle,
        sourceType = sourceId.toSourceTypeLabel(),
        musicCount = musicCount,
    )
}

private fun SourceId.toSourceTypeLabel(): String {
    return when (this) {
        BuiltInSourceIds.WebDav -> "WebDAV"
        BuiltInSourceIds.OneDrive -> "OneDrive"
        BuiltInSourceIds.Local -> "Local"
        BuiltInSourceIds.Navidrome -> "Navidrome"
        BuiltInSourceIds.OpenSubsonic -> "OpenSubsonic"
        BuiltInSourceIds.Emby -> "Emby"
        else -> value.replaceFirstChar { char -> char.uppercase() }
    }
}
