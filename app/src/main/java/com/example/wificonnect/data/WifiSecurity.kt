package com.example.wificonnect.data

/** Tipos de segurança suportados neste exemplo de conexão local. */
enum class WifiSecurity(val displayName: String) {
    OPEN("Aberta (sem senha)"),
    WPA2_PSK("WPA2-Personal"),
    WPA3_SAE("WPA3-Personal"),
    OWE("Enhanced Open (OWE)"),
}
