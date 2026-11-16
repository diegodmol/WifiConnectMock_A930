package com.example.wificonnect.data

/** A rede pode ser desconhecida (não salva) pelo Android. */
data class WifiNetwork(
    val ssid: String,
    val securityType: WifiSecurity,
    val password: String? = null,
) {
    /** Validação local, sem dependências do Android; facilita testar a configuração mockada. */
    fun validationError(): String? {
        if (ssid.isBlank() || ssid.toByteArray(Charsets.UTF_8).size > 32) {
            return "SSID inválido (precisa ter de 1 a 32 bytes)."
        }
        return when (securityType) {
            WifiSecurity.OPEN, WifiSecurity.OWE -> {
                if (!password.isNullOrEmpty()) "A rede ${securityType.displayName} não deve ter senha." else null
            }
            WifiSecurity.WPA2_PSK, WifiSecurity.WPA3_SAE -> {
                if (password == null || password.length !in 8..63 || password.any { it.code !in 32..126 }) {
                    "A senha de ${securityType.displayName} precisa ter de 8 a 63 caracteres ASCII imprimíveis."
                } else null
            }
        }
    }
}
