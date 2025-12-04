package com.silentport.silentport.firewall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.silentport.silentport.SilentPortApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FirewallNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        if (intent.action == ACTION_DISABLE_FIREWALL) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val controller = FirewallControllerProvider.get(context.applicationContext)
                    controller.disableFirewall()
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (intent.action == ACTION_ALLOW_APP) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            if (packageName != null) {
                // Cancel the notification immediately
                val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                notificationManager.cancel(packageName.hashCode())

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Bug fix 13: Use singleton data source from AppContainer
                        val app = context.applicationContext as SilentPortApp
                        val prefs = app.container.settingsPreferences
                        val duration = prefs.preferencesFlow.first().allowDurationMillis
                        val controller = FirewallControllerProvider.get(context.applicationContext)
                        controller.temporarilyUnblock(packageName, duration)
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                pendingResult.finish()
            }
        } else {
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_DISABLE_FIREWALL = "com.silentport.silentport.firewall.DISABLE"
        const val ACTION_ALLOW_APP = "com.silentport.silentport.firewall.ALLOW_APP"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}

