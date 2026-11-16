package com.example.wificonnect.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WifiScreen(
    state: WifiUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    MaterialTheme {
        Surface(color = Color(0xFFF7F9FC), modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(86.dp)
                        .background(Color(0xFFE1ECFF), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("📶", fontSize = 42.sp) }

                Spacer(Modifier.height(24.dp))
                Text("Wi-Fi Connect", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Conexão com dados mockados", color = Color(0xFF667085))
                Spacer(Modifier.height(28.dp))

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("REDE CONFIGURADA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
                        Spacer(Modifier.height(6.dp))
                        Text(state.ssid, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Segurança: ${state.securityType.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475467),
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = when (state.status) {
                                WifiStatus.IDLE -> "● Aguardando"
                                WifiStatus.CONNECTING -> "● Conectando"
                                WifiStatus.CONNECTED -> "● Conectado"
                                WifiStatus.ERROR -> "● Não conectado"
                            },
                            color = when (state.status) {
                                WifiStatus.CONNECTED -> Color(0xFF067647)
                                WifiStatus.ERROR -> Color(0xFFB42318)
                                WifiStatus.CONNECTING -> Color(0xFF175CD3)
                                WifiStatus.IDLE -> Color(0xFF667085)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = Color(0xFF475467), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onConnect,
                    enabled = state.status != WifiStatus.CONNECTING && state.status != WifiStatus.CONNECTED,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF175CD3)),
                ) {
                    if (state.status == WifiStatus.CONNECTING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Conectar ao Wi-Fi")
                    }
                }
                if (state.status == WifiStatus.CONNECTED) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                        Text("Desconectar")
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "O Android pode solicitar autorização. A conexão é local e não altera necessariamente a internet padrão do aparelho.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
