package com.example.batteryanalyzer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.batteryanalyzer.ui.AppUsageHome
import com.example.batteryanalyzer.ui.theme.BatteryAnalyzerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as BatteryAnalyzerApp
        MainViewModel.Factory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryAnalyzerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val uiState by viewModel.uiState.collectAsState()
                    AppUsageHome(
                        state = uiState,
                        onRequestUsagePermission = { openUsageAccessSettings() },
                        onRefresh = { viewModel.refreshUsage() },
                        onRestoreApp = { packageName ->
                            lifecycleScope.launch { viewModel.restoreApp(packageName) }
                        },
                        onOpenAppInfo = { packageName -> openAppDetails(packageName) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { viewModel.refreshUsage() }
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun openAppDetails(packageName: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
