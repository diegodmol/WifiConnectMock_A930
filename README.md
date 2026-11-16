# WiFiConnectMock — compatível com PAX A930 (Android 7)

Aplicativo de exemplo **Kotlin + Jetpack Compose + MVVM + Repository mockado**.
Ao tocar em **Conectar ao Wi-Fi**, lê do `MockWifiRepository` o **SSID**, a **senha** e o **tipo de segurança** e tenta a conexão real.

## Correção para a PAX A930

A documentação PagBank informa que o terminal A930 utiliza **Android 7**. A classe `WifiNetworkSpecifier.Builder` só foi adicionada no Android 10 (API 29). Por isso a versão anterior fechava com `NoClassDefFoundError` ao tocar no botão.

Esta versão usa:

- Android **7, 8, 9 (API 24 a 28):** `LegacyWifiConnector` com `WifiManager.addNetwork()`, `enableNetwork()` e `reconnect()`. A rede é cadastrada no terminal e passa a poder ser usada como Wi-Fi do sistema. Detectamos a conexão por broadcasts; não consideramos `reconnect()` igual a sucesso.
- Android **10+ (API 29+):** `Android10WifiConnector` com `WifiNetworkSpecifier`. Pode exigir confirmação e normalmente é uma conexão local para uso pelo aplicativo.
- `AndroidWifiConnector` seleciona automaticamente a implementação conforme a versão da API.

## Tipos de segurança

| Segurança | Android 7–9 | Android 10+ |
|---|---|---|
| `OPEN` | Sim | Sim |
| `WPA2_PSK` | Sim | Sim |
| `WPA3_SAE` | Não: erro tratado | Depende de suporte do terminal |
| `OWE` | Não: erro tratado | Depende de suporte do terminal |

O exemplo não implementa redes corporativas WPA-Enterprise/EAP ou WEP. Use WPA2-Personal para testar na A930.

## Personalização

Edite `app/src/main/java/com/example/wificonnect/data/MockWifiRepository.kt`:

```kotlin
class MockWifiRepository : WifiRepository {
    override fun getNetwork() = WifiNetwork(
        ssid = "MINHA_REDE_WIFI",
        securityType = WifiSecurity.WPA2_PSK,
        password = "Senha1234",
    )
}
```

`WifiNetwork` valida SSID e senha. Para OPEN, use `password = null`.

## Como testar na PAX A930

1. Configure uma rede **WPA2-Personal** ao alcance do terminal e atualize SSID/senha no mock.
2. Abra a pasta do projeto no Android Studio. O `minSdk` agora é **24 (Android 7)**.
3. Verifique que o Wi-Fi e, se necessário, a localização do terminal estão ativados; autorize a permissão de localização quando o app solicitar.
4. Instale a APK pela forma autorizada no seu equipamento e toque em **Conectar ao Wi-Fi**.
5. Observe o status e filtre o Logcat por `WifiConnector` se houver erro.

Para conferir a versão da API:

```bash
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
```

## Atenções

- Em SmartPOS de produção, políticas/firmware do fabricante ou do adquirente podem bloquear `addNetwork`, `enableNetwork` e instalações de terceiros, mesmo com permissões.
- `addNetwork` adiciona a rede às configurações Wi-Fi da SmartPOS no Android 7–9; esta versão tenta conectar e só informa sucesso após confirmar a rede. **O método `disconnect()` pode desconectar a rede atual do terminal ao tocar no botão.** Encerrar o app não derruba o Wi-Fi no Android legado.
- `WifiNetworkSpecifier` no Android 10+ não troca necessariamente a conexão padrão do terminal.
- O projeto usa as versões de Gradle/AndroidX do pacote anterior e não inclui o executável JAR do Gradle Wrapper. Instale ou gere o Wrapper se o seu Android Studio solicitar.
- Não foi possível executar uma compilação Android completa neste ambiente. Os testes devem ser realizados no Android Studio, de preferência com um terminal de desenvolvimento autorizado.

Referências: [PagBank — FAQ SmartPOS](https://developer.pagbank.com.br/docs/faq-smartpos) e [Android — WifiManager](https://developer.android.com/reference/android/net/wifi/WifiManager).
