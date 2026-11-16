package com.example.wificonnect.data

/** Apenas a ORIGEM dos dados é mockada. A tentativa de conexão é real. */
class MockWifiRepository : WifiRepository {
    override fun getNetwork() = WifiNetwork(
        ssid = "MINHA_REDE_WIFI",
        securityType = WifiSecurity.WPA2_PSK,
        password = "Senha1234",
    )
}
