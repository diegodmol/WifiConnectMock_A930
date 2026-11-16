package com.example.wificonnect.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wificonnect.data.WifiRepository
import com.example.wificonnect.platform.WifiConnector
import com.example.wificonnect.platform.WifiEvent

class WifiViewModel(
    private val repository: WifiRepository,
    private val connector: WifiConnector,
) : ViewModel() {
    private val targetNetwork = repository.getNetwork()

    var uiState by mutableStateOf(WifiUiState(ssid = targetNetwork.ssid, securityType = targetNetwork.securityType))
        private set

    fun connect() {
        if (uiState.status == WifiStatus.CONNECTING || uiState.status == WifiStatus.CONNECTED) return

        uiState = uiState.copy(
            status = WifiStatus.CONNECTING,
            message = "Aguardando confirmação/conexão do Android…",
        )
        connector.connect(targetNetwork) { event ->
            uiState = when (event) {
                WifiEvent.Connected -> uiState.copy(
                    status = WifiStatus.CONNECTED,
                    message = "Conexão local estabelecida com ${targetNetwork.ssid}.",
                )
                WifiEvent.Disconnected -> uiState.copy(
                    status = WifiStatus.IDLE,
                    message = "A conexão Wi-Fi foi perdida.",
                )
                is WifiEvent.Failure -> uiState.copy(
                    status = WifiStatus.ERROR,
                    message = event.reason,
                )
            }
        }
    }

    fun disconnect() {
        connector.disconnect()
        uiState = uiState.copy(
            status = WifiStatus.IDLE,
            message = "Solicitação de conexão encerrada.",
        )
    }

    fun onPermissionDenied() {
        uiState = uiState.copy(
            status = WifiStatus.ERROR,
            message = "Permissão necessária para solicitar esta conexão.",
        )
    }

    override fun onCleared() {
        connector.release()
        super.onCleared()
    }

    class Factory(
        private val repository: WifiRepository,
        private val connector: WifiConnector,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WifiViewModel::class.java))
            return WifiViewModel(repository, connector) as T
        }
    }
}
