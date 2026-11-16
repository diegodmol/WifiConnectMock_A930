package com.example.wificonnect.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.wificonnect.data.WifiNetwork
import com.example.wificonnect.data.WifiSecurity

/**
 * Conecta a uma rede local não salva no Android 10+.
 * O sistema pode solicitar aprovação; não muda a rede padrão de internet do aparelho.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class Android10WifiConnector(context: Context) : WifiConnector {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeCallback: ConnectivityManager.NetworkCallback? = null
    private var requestId = 0

    override fun connect(network: WifiNetwork, onEvent: (WifiEvent) -> Unit) {
        disconnect() // Cancela a solicitação anterior.

        if (wifiManager?.isWifiEnabled != true) {
            onEvent(WifiEvent.Failure("Ative o Wi-Fi do celular e tente novamente."))
            return
        }
        network.validationError()?.let {
            onEvent(WifiEvent.Failure(it))
            return
        }
        if (connectivityManager == null) {
            onEvent(WifiEvent.Failure("Conectividade indisponível neste dispositivo."))
            return
        }

        when (network.securityType) {
            WifiSecurity.WPA3_SAE -> {
                if (!wifiManager.isWpa3SaeSupported) {
                    onEvent(WifiEvent.Failure("Este dispositivo não oferece suporte a WPA3-SAE."))
                    return
                }
            }
            WifiSecurity.OWE -> {
                if (!wifiManager.isEnhancedOpenSupported) {
                    onEvent(WifiEvent.Failure("Este dispositivo não oferece suporte a Enhanced Open (OWE)."))
                    return
                }
            }
            else -> Unit
        }

        val specifier = try {
            WifiNetworkSpecifier.Builder()
                .setSsid(network.ssid)
                .apply {
                    when (network.securityType) {
                        WifiSecurity.OPEN -> Unit // Sem senha e sem segurança adicional.
                        WifiSecurity.WPA2_PSK -> setWpa2Passphrase(requireNotNull(network.password))
                        WifiSecurity.WPA3_SAE -> setWpa3Passphrase(requireNotNull(network.password))
                        WifiSecurity.OWE -> setIsEnhancedOpen(true)
                    }
                }
                .build()
        } catch (_: IllegalArgumentException) {
            onEvent(WifiEvent.Failure("Configuração de SSID, senha ou segurança inválida."))
            return
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val currentId = ++requestId
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Conexão estabelecida: network=$network")
                deliverIfCurrent(currentId) { onEvent(WifiEvent.Connected) }
            }

            override fun onUnavailable() {
                Log.w(TAG, "Rede indisponível (SSID=${network.ssid})")
                deliverIfCurrent(currentId) {
                    clearCallback(this)
                    onEvent(WifiEvent.Failure("Rede não encontrada, conexão recusada ou tempo esgotado."))
                }
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Conexão Wi-Fi perdida: network=$network")
                deliverIfCurrent(currentId) {
                    clearCallback(this)
                    onEvent(WifiEvent.Disconnected)
                }
            }
        }
        activeCallback = callback
        try {
            Log.d(TAG, "Solicitando conexão Wi-Fi SSID=${network.ssid}, segurança=${network.securityType}")
            connectivityManager.requestNetwork(request, callback, 30_000)
        } catch (error: SecurityException) {
            // Não houve registro bem-sucedido do callback; não tente desregistrá-lo.
            if (activeCallback === callback) activeCallback = null
            Log.e(TAG, "Permissão insuficiente ao solicitar rede Wi-Fi", error)
            onEvent(WifiEvent.Failure("Permissão insuficiente. Confira CHANGE_NETWORK_STATE e a permissão de Wi-Fi próxima/localização."))
        } catch (error: RuntimeException) {
            if (activeCallback === callback) activeCallback = null
            Log.e(TAG, "Falha ao solicitar rede Wi-Fi", error)
            onEvent(WifiEvent.Failure("O Android recusou a solicitação. Consulte o Logcat (filtro WifiConnector)."))
        }
    }

    override fun disconnect() {
        requestId++ // Ignora callbacks atrasados.
        activeCallback?.let(::clearCallback)
    }

    private fun clearCallback(callback: ConnectivityManager.NetworkCallback) {
        if (activeCallback === callback) activeCallback = null
        try {
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (_: IllegalArgumentException) {
            // O Android pode já ter encerrado a solicitação.
        }
    }

    private companion object {
        const val TAG = "WifiConnector"
    }

    private fun deliverIfCurrent(id: Int, action: () -> Unit) {
        mainHandler.post {
            if (id == requestId) action()
        }
    }
}
