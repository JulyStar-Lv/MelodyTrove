package com.github.tidetunes.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.NextPage -> {
                val current = _state.value.currentPage
                val pages = OnboardingPage.entries
                if (current < pages.last().index) {
                    _state.value = _state.value.copy(currentPage = current + 1)
                }
            }
            OnboardingAction.PreviousPage -> {
                val current = _state.value.currentPage
                if (current > 0) {
                    _state.value = _state.value.copy(currentPage = current - 1)
                }
            }
            OnboardingAction.Finish -> {
                _state.value = _state.value.copy(isComplete = true)
                viewModelScope.launch {
                    _events.send(OnboardingEvent.OnboardingComplete)
                }
            }
            OnboardingAction.NavigateToSources -> {
                viewModelScope.launch {
                    _events.send(OnboardingEvent.NavigateToSources)
                }
            }
        }
    }
}
