package com.example.wificonnect.platform

import com.example.wificonnect.data.WifiNetwork

sealed interface WifiEvent {
    data object Connected : WifiEvent
    data object Disconnected : WifiEvent
    data class Failure(val reason: String) : WifiEvent
}

/** Abstração do mecanismo de conexão, independente da tela e do repositório. */
interface WifiConnector {
    fun connect(network: WifiNetwork, onEvent: (WifiEvent) -> Unit)
    fun disconnect()
    /** Libera monitoramento. No legado, não derruba a conexão ao fechar o app. */
    fun release() = disconnect()
}
