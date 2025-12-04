package com.silentport.silentport

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.silentport.silentport.ui.SilentPortRoot
import com.silentport.silentport.ui.theme.SilentPortTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as SilentPortApp
        MainViewModel.Factory(app.container)
    }

    private enum class PendingAction {
        ENABLE, ALLOW_DURATION, BLOCK_NOW, NONE
    }

    private var pendingAction = PendingAction.NONE

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val permissionGranted = result.resultCode == Activity.RESULT_OK || VpnService.prepare(this) == null
        if (permissionGranted) {
            executePendingAction()
        }
        pendingAction = PendingAction.NONE
    }

    private fun executePendingAction() {
        when (pendingAction) {
            PendingAction.ENABLE -> lifecycleScope.launch { viewModel.enableFirewall() }
            PendingAction.ALLOW_DURATION -> lifecycleScope.launch { viewModel.allowForConfiguredDuration() }
            PendingAction.BLOCK_NOW -> lifecycleScope.launch { viewModel.blockNow() }
            PendingAction.NONE -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            val actionName = savedInstanceState.getString("pending_action")
            if (actionName != null) {
                pendingAction = PendingAction.valueOf(actionName)
            }
        }
        setContent {
            SilentPortTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SilentPortRoot(
                        uiStateFlow = viewModel.uiState,
                        onRequestUsagePermission = { openUsageAccessSettings() },
                        onRefresh = { viewModel.refreshUsage() },

                        onOpenAppInfo = { packageName -> openAppDetails(packageName) },
                        onEnableFirewall = {
                            ensureVpnPermission(PendingAction.ENABLE)
                        },
                        onDisableFirewall = {
                            lifecycleScope.launch { viewModel.disableFirewall() }
                        },
                        onAllowForDuration = {
                            ensureVpnPermission(PendingAction.ALLOW_DURATION)
                        },
                        onBlockNow = {
                            ensureVpnPermission(PendingAction.BLOCK_NOW)
                        },
                        onUpdateAllowDuration = { duration ->
                            lifecycleScope.launch { viewModel.updateAllowDuration(duration) }
                        },
                        onToggleMetrics = { enabled ->
                            lifecycleScope.launch { viewModel.setMetricsEnabled(enabled) }
                        },
                        onManualFirewallUnblockChange = { enabled ->
                            lifecycleScope.launch { viewModel.setManualFirewallUnblock(enabled) }
                        },
                        onManualFirewallUnblock = { packageName ->
                            lifecycleScope.launch { viewModel.manualUnblockPackage(packageName) }
                        },
                        onRefreshMetrics = {
                            lifecycleScope.launch { viewModel.refreshMetricsNow() }
                        },
                        onAddToWhitelist = { packageName ->
                            lifecycleScope.launch { viewModel.addToWhitelist(packageName) }
                        },
                        onRemoveFromWhitelist = { packageName ->
                            lifecycleScope.launch { viewModel.removeFromWhitelist(packageName) }
                        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pending_action", pendingAction.name)
    }

    private fun ensureVpnPermission(action: PendingAction) {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent == null) {
            pendingAction = action
            executePendingAction()
            pendingAction = PendingAction.NONE
        } else {
            pendingAction = action
            vpnPermissionLauncher.launch(prepareIntent)
        }
    }
}
