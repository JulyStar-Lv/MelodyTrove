package io.github.julystar.musicapp.feature.radio.presentation

sealed interface RadioEvent {
    data class ShowMessage(val message: String) : RadioEvent
}
