package com.example.wificonnect.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.wificonnect.data.WifiNetwork
import com.example.wificonnect.data.WifiSecurity

/**
 * Conexão Wi-Fi para Android 7, 8 e 9 (API 24–28).
 * Usa a API legada porque WifiNetworkSpecifier NÃO existe nesses Androids.
 * addNetwork salva a configuração no dispositivo.
 */
@Suppress("DEPRECATION")
class LegacyWifiConnector(context: Context) : WifiConnector {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())

    private var receiver: BroadcastReceiver? = null
    private var timeout: Runnable? = null
    private var pendingNetworkId: Int? = null
    private var activeSsid: String? = null
    private var onEvent: ((WifiEvent) -> Unit)? = null
    private var connected = false

    override fun connect(network: WifiNetwork, onEvent: (WifiEvent) -> Unit) {
        release()
        network.validationError()?.let {
            onEvent(WifiEvent.Failure(it))
            return
        }
        if (network.securityType == WifiSecurity.WPA3_SAE || network.securityType == WifiSecurity.OWE) {
            onEvent(WifiEvent.Failure("Android 7–9: utilize rede aberta ou WPA2-PSK."))
            return
        }
        if (!wifiManager.isWifiEnabled) {
            onEvent(WifiEvent.Failure("Ative o Wi-Fi da PAX e tente novamente."))
            return
        }

        this.onEvent = onEvent
        val config = WifiConfiguration().apply {
            SSID = "\"${network.ssid}\""
            when (network.securityType) {
                WifiSecurity.OPEN -> allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                WifiSecurity.WPA2_PSK -> {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    preSharedKey = "\"${network.password}\""
                }
                else -> error("Tipo não suportado no Android legado")
            }
        }

        try {
            val networkId = wifiManager.addNetwork(config)
            if (networkId == -1) {
                finishWith(WifiEvent.Failure("A PAX recusou cadastrar esta rede (addNetwork=-1). Confira restrições do fabricante."))
                return
            }

            pendingNetworkId = networkId
            activeSsid = network.ssid
            registerReceiver()

            val enabled = wifiManager.enableNetwork(networkId, true)
            val reconnectRequested = enabled && wifiManager.reconnect()
            Log.d(TAG, "Rede solicitada: SSID=${network.ssid}, id=$networkId, enabled=$enabled, reconnect=$reconnectRequested")
            if (!reconnectRequested) {
                finishWith(WifiEvent.Failure("Não foi possível iniciar a conexão Wi-Fi (enableNetwork/reconnect)."))
                return
            }

            checkConnected()
            if (!connected) timeout = Runnable {
                if (this.onEvent != null && !connected) {
                    finishWith(WifiEvent.Failure("Tempo esgotado ao conectar a ${network.ssid}. Verifique senha, sinal e políticas da PAX."))
                }
            }.also { handler.postDelayed(it, 25_000L) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para conectar Wi-Fi", e)
            finishWith(WifiEvent.Failure("O sistema bloqueou a conexão: ${e.message ?: "permissão insuficiente"}"))
        } catch (e: RuntimeException) {
            Log.e(TAG, "Erro ao conectar Wi-Fi legado", e)
            finishWith(WifiEvent.Failure("Falha ao conectar: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    private fun registerReceiver() {
        val observer = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return
                @Suppress("DEPRECATION")
                val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                when {
                    info?.isConnected == true -> checkConnected()
                    connected && info?.isConnected == false ->
                        finishWith(WifiEvent.Disconnected)
                }
            }
        }
        appContext.registerReceiver(observer, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        receiver = observer
    }

    private fun checkConnected() {
        if (onEvent == null || connected) return
        val ssid = wifiManager.connectionInfo?.ssid?.trim('"')
        val wifiConnected = connectivityManager
            .getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected == true
        if (wifiConnected && ssid == activeSsid) {
            connected = true
            Log.d(TAG, "Conectado ao Wi-Fi esperado: $ssid")
            val callback = onEvent
            timeout?.let(handler::removeCallbacks)
            timeout = null
            // Mantém o receiver e o callback para detectar perda da rede.
            callback?.invoke(WifiEvent.Connected)
        }
    }

    private fun finishWith(event: WifiEvent) {
        val callback = onEvent
        onEvent = null
        release()
        callback?.invoke(event)
    }

    override fun disconnect() {
        // Ação explícita do usuário: desconecta, sem apagar a rede salva na PAX.
        val callback = onEvent
        onEvent = null
        val id = pendingNetworkId
        release()
        try {
            wifiManager.disconnect()
            if (id != null) wifiManager.disableNetwork(id)
        } catch (e: SecurityException) {
            Log.e(TAG, "Não foi possível desconectar Wi-Fi", e)
        }
        callback?.invoke(WifiEvent.Disconnected)
    }

    override fun release() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        receiver?.let { current ->
            try {
                appContext.unregisterReceiver(current)
            } catch (_: IllegalArgumentException) {
                // Já desregistrado.
            }
        }
        receiver = null
        pendingNetworkId = null
        activeSsid = null
        onEvent = null
        connected = false
    }

    private companion object {
        const val TAG = "WifiConnector"
    }
}
