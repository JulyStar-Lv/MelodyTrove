package com.github.tidetunes.core.data.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.github.tidetunes.core.domain.model.NetworkStatus
import com.github.tidetunes.core.domain.repository.NetworkStatusProvider
import com.github.tidetunes.platform.appContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidNetworkStatusProvider : NetworkStatusProvider {
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mutableStatus = MutableStateFlow(readStatus())

    override val status: StateFlow<NetworkStatus> = mutableStatus.asStateFlow()

    init {
        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refresh()
                override fun onLost(network: Network) = refresh()
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) = refresh()
            }
        )
    }

    private fun refresh() {
        mutableStatus.value = readStatus()
    }

    private fun readStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        return NetworkStatus(
            isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            isMetered = connectivityManager.isActiveNetworkMetered,
        )
    }
}
