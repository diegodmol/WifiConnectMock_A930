package com.example.wificonnect.platform

import android.content.Context
import android.os.Build
import com.example.wificonnect.data.WifiNetwork

/**
 * Escolhe uma API existente no Android instalado.
 * WifiNetworkSpecifier só existe no Android 10+ (API 29).
 */
class AndroidWifiConnector(context: Context) : WifiConnector {
    private val delegate: WifiConnector = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Android10WifiConnector(context)
    } else {
        LegacyWifiConnector(context)
    }

    override fun connect(network: WifiNetwork, onEvent: (WifiEvent) -> Unit) =
        delegate.connect(network, onEvent)

    override fun disconnect() = delegate.disconnect()

    override fun release() = delegate.release()
}
