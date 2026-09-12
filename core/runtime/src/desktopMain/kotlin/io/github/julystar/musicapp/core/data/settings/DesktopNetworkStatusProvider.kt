package io.github.julystar.musicapp.core.data.settings

import io.github.julystar.musicapp.core.domain.model.NetworkStatus
import io.github.julystar.musicapp.core.domain.repository.NetworkStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DesktopNetworkStatusProvider : NetworkStatusProvider {
    override val status: StateFlow<NetworkStatus> = MutableStateFlow(NetworkStatus.Unknown)
}
