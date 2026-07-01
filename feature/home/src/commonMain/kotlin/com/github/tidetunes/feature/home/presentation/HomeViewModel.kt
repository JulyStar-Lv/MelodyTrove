package com.github.tidetunes.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)

    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        viewModelScope.launch {
            _events.send(
                when (action) {
                    HomeAction.NavigateToDownloads -> HomeEvent.NavigateToDownloads
                    HomeAction.NavigateToLibrary -> HomeEvent.NavigateToLibrary
                    HomeAction.NavigateToSearch -> HomeEvent.NavigateToSearch
                    HomeAction.OpenSleepTimer -> HomeEvent.OpenSleepTimer
                    HomeAction.OpenNowPlaying -> HomeEvent.OpenNowPlaying
                },
            )
        }
    }
}
