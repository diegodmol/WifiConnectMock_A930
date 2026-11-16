package com.example.wificonnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.wificonnect.data.MockWifiRepository
import com.example.wificonnect.platform.AndroidWifiConnector
import com.example.wificonnect.presentation.WifiScreen
import com.example.wificonnect.presentation.WifiViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WifiViewModel by viewModels {
        WifiViewModel.Factory(
            repository = MockWifiRepository(),
            connector = AndroidWifiConnector(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permission = if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.NEARBY_WIFI_DEVICES
            } else {
                Manifest.permission.ACCESS_FINE_LOCATION
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted) viewModel.connect() else viewModel.onPermissionDenied()
            }

            WifiScreen(
                state = viewModel.uiState,
                onConnect = {
                    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.connect()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                },
                onDisconnect = viewModel::disconnect,
            )
        }
    }
}
