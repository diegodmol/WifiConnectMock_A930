package com.example.wificonnect.data

/** Abstrai a origem dos dados da rede (mock, API, banco etc.). */
interface WifiRepository {
    fun getNetwork(): WifiNetwork
}
