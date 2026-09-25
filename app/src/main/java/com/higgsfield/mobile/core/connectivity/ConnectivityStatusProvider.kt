package com.higgsfield.mobile.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConnectivityStatusProvider {
    val isOnline: StateFlow<Boolean>
}

@Singleton
class AndroidConnectivityStatusProvider @Inject constructor(
    @ApplicationContext context: Context,
) : ConnectivityStatusProvider {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val mutableIsOnline = MutableStateFlow(isValidated(connectivityManager.activeNetwork))
    override val isOnline: StateFlow<Boolean> = mutableIsOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            mutableIsOnline.value = capabilities.isValidatedInternet()
        }

        override fun onLost(network: Network) {
            mutableIsOnline.value = isValidated(connectivityManager.activeNetwork)
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun isValidated(network: Network?): Boolean =
        network?.let(connectivityManager::getNetworkCapabilities)?.isValidatedInternet() == true
}

private fun NetworkCapabilities.isValidatedInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

object AlwaysOnlineConnectivityStatusProvider : ConnectivityStatusProvider {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}
