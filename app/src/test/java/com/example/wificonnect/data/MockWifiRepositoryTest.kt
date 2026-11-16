package com.example.wificonnect.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class MockWifiRepositoryTest {
    @Test
    fun `should return WPA2 network with SSID and password`() {
        val network = MockWifiRepository().getNetwork()
        assertEquals("MINHA_REDE_WIFI", network.ssid)
        assertEquals("Senha1234", network.password)
        assertEquals(WifiSecurity.WPA2_PSK, network.securityType)
        assertNull(network.validationError())
    }

    @Test
    fun `open and OWE networks do not require password`() {
        assertNull(WifiNetwork("WifiAberto", WifiSecurity.OPEN).validationError())
        assertNull(WifiNetwork("WifiOWE", WifiSecurity.OWE).validationError())
    }

    @Test
    fun `protected networks must have valid password`() {
        assertNotNull(WifiNetwork("Rede", WifiSecurity.WPA2_PSK).validationError())
        assertNotNull(WifiNetwork("Rede", WifiSecurity.WPA3_SAE, "abc").validationError())
        assertNull(WifiNetwork("Rede", WifiSecurity.WPA3_SAE, "Senha1234").validationError())
    }

    @Test
    fun `open network rejects unexpected password`() {
        assertNotNull(WifiNetwork("WifiAberto", WifiSecurity.OPEN, "Senha1234").validationError())
    }
}
