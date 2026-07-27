package com.github.tidetunes.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.feature.home.domain.HomeStatisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListeningViewModel(
    private val statisticsRepository: HomeStatisticsRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(ListeningTab.Overview)

    val state = combine(
        statisticsRepository.listeningStatistics,
        libraryRepository.tracks,
        libraryRepository.initialLoadComplete,
        selectedTab,
    ) { statistics, tracks, initialLoadComplete, tab ->
        buildListeningState(
            snapshot = statistics,
            libraryTracks = tracks,
            selectedTab = tab,
            isLoading = !initialLoadComplete,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ListeningState(),
    )

    fun onAction(action: ListeningAction) {
        when (action) {
            is ListeningAction.SelectTab -> selectedTab.value = action.tab
            is ListeningAction.RemoveHistoryEntry -> viewModelScope.launch {
                statisticsRepository.removeHistoryEntry(action.id)
            }
            ListeningAction.NavigateBack,
            is ListeningAction.PlayTrack -> Unit
        }
    }
}
