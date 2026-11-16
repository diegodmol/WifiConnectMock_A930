package com.example.wificonnect.presentation

import com.example.wificonnect.data.WifiSecurity

enum class WifiStatus { IDLE, CONNECTING, CONNECTED, ERROR }

data class WifiUiState(
    val ssid: String,
    val securityType: WifiSecurity,
    val status: WifiStatus = WifiStatus.IDLE,
    val message: String = "Pronto para solicitar a conexão.",
)
