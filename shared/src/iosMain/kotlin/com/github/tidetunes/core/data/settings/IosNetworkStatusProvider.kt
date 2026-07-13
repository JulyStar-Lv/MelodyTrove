package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.domain.model.NetworkStatus
import com.github.tidetunes.core.domain.repository.NetworkStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IosNetworkStatusProvider : NetworkStatusProvider {
    override val status: StateFlow<NetworkStatus> = MutableStateFlow(NetworkStatus.Unknown)
}
