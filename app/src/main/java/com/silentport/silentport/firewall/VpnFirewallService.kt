package com.silentport.silentport.firewall

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.silentport.silentport.MainActivity
import com.silentport.silentport.R
import java.io.IOException
import java.io.InputStream

class VpnFirewallService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null
    private var inputStream: InputStream? = null
    @Volatile
    private var blockedPackages: Set<String> = emptySet()
    private var isBlockingMode: Boolean = false


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action")
        if (action == ACTION_STOP) {
            Log.i(TAG, "Stopping firewall service")
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_OR_UPDATE) {
            val blocking = intent.getBooleanExtra(EXTRA_BLOCKING, false)
            isBlockingMode = blocking
            val providedList = intent.getStringArrayListExtra(EXTRA_BLOCK_LIST)
            
            if (blocking && (providedList == null || providedList.isEmpty())) {
                 // Bug fix 2: If list is missing but blocking is requested, read from prefs
                 kotlinx.coroutines.runBlocking {
                     val prefs = FirewallPreferencesDataSource(applicationContext)
                     blockedPackages = kotlinx.coroutines.flow.first(prefs.preferencesFlow).blockedPackages
                 }
            } else {
                blockedPackages = when {
                    blocking -> providedList?.toSet() ?: emptySet()
                    providedList != null -> providedList.toSet()
                    else -> emptySet()
                }
            }

            Log.i(TAG, "Starting/Updating firewall: blocking=$blocking blocked=${blockedPackages.size}")
            startForeground(NOTIFICATION_ID, buildNotification(blocking))
            updateVpn()
            return START_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "VpnFirewallService destroyed")
        stopVpn()
    }

    private fun buildNotification(isBlocking: Boolean): Notification {
        val channelId = FIREWALL_CHANNEL_ID
        
        val disableIntent = Intent(this, FirewallNotificationReceiver::class.java).apply {
            action = FirewallNotificationReceiver.ACTION_DISABLE_FIREWALL
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(getString(R.string.firewall_notification_persistent_title))
            .setContentText(getString(R.string.firewall_notification_persistent_text))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(
                NotificationCompat.Action(
                    0,
                    getString(R.string.firewall_notification_action_disable),
                    disablePendingIntent
                )
            )
            .build()
    }

    private fun updateVpn() {
        Log.d(TAG, "updateVpn blockingMode=$isBlockingMode blockedPackages=${blockedPackages.size}")
        
        // Optimization: If we are not blocking and not in blocking mode, just ensure stopped
        if (!isBlockingMode && blockedPackages.isEmpty()) {
             if (vpnInterface != null) {
                 Log.d(TAG, "No blocking required -> stopping VPN")
                 stopVpn()
             }
             return
        }

        // For now, we still restart to apply changes, but we could optimize further to only restart if diff is significant
        // or use addDisallowedApplication dynamically if API allowed (it doesn't for active VPN).
        // To minimize downtime, we could try to establish new before closing old, but Android VpnService usually requires
        // only one active interface per app or automatically closes the old one.
        // We will stick to stop -> start for reliability but ensure it's fast.
        
        when {
            isBlockingMode -> {
                Log.d(TAG, "Activating global block mode")
                startGlobalBlockVpn()
            }
            else -> {
                Log.d(TAG, "Activating selective block mode")
                startSelectiveBlockVpn()
            }
        }
    }

    private fun startGlobalBlockVpn() {
        stopVpn()
        val builder = createBaseBuilder()

        // Bug fix: No need to explicitly exclude self if we are the VPN provider, 
        // but it's good practice to ensure we don't block our own traffic if we were to block everything.
        // However, in selective block mode it was redundant. Here it is fine.
        runCatching { builder.addDisallowedApplication(packageName) }
            .onFailure { Log.w(TAG, "Unable to exclude app from VPN", it) }

        establishVpn(builder)
    }

    private fun startSelectiveBlockVpn() {
        if (blockedPackages.isEmpty()) {
            Log.w(TAG, "startSelectiveBlockVpn called with empty list")
            stopVpn()
            return
        }

        stopVpn()
        val builder = createBaseBuilder()

        var addedCount = 0
        for (pkg in blockedPackages) {
             // Bug fix: Removed redundant check (pkg != packageName) as we don't add ourselves to blockedPackages usually
             runCatching {
                 builder.addAllowedApplication(pkg)
                 addedCount++
                 // Log.v(TAG, "Selective block -> app routed through VPN: $pkg") // Reduced logging
             }.onFailure { Log.w(TAG, "Unable to include $pkg in VPN", it) }
        }
        Log.i(TAG, "Added $addedCount packages to VPN")

        establishVpn(builder)
    }

    private fun createBaseBuilder(): Builder {
        val builder = Builder()
            .setSession(getString(R.string.firewall_vpn_session_name))

        builder.addAddress(VPN_ADDRESS, 32)
        builder.addRoute("0.0.0.0", 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            builder.addAddress("fd00:1:fd00::1", 128)
            builder.addRoute("::", 0)
        }

        return builder
    }

    private fun establishVpn(builder: Builder) {
        try {
            vpnInterface = builder.establish()
            vpnInterface?.let { descriptor ->
                Log.i(TAG, "VPN interface established")
                inputStream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                drainPackets()
            } ?: Log.w(TAG, "Failed to establish VPN interface")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        drainThread?.interrupt()
        drainThread = null

        try {
            inputStream?.close()
        } catch (ex: IOException) {
            // Log.w(TAG, "Error closing input stream", ex) // Benign
        }
        inputStream = null

        try {
            vpnInterface?.close()
        } catch (ex: IOException) {
            Log.w(TAG, "Error closing VPN interface", ex)
        }
        vpnInterface = null
    }

    private fun drainPackets() {
        val stream = inputStream ?: return
        drainThread?.interrupt()
        drainThread = Thread {
            val buffer = ByteArray(32767)
            val monitor = ForegroundAppMonitor(this)
            var lastNotificationTime = 0L
            val notificationCooldown = 5000L // 5 seconds

            while (!Thread.currentThread().isInterrupted) {
                try {
                    val read = stream.read(buffer)
                    if (read <= 0) {
                        Log.v(TAG, "Firewall VPN drain thread idle")
                        Thread.sleep(100)
                    } else {
                        // Traffic detected from a blocked app
                        val now = System.currentTimeMillis()
                        if (now - lastNotificationTime > notificationCooldown) {
                            val foregroundApp = monitor.getForegroundApp()
                            if (foregroundApp != null && blockedPackages.contains(foregroundApp)) {
                                Log.i(TAG, "Blocked app detected in foreground") // Privacy fix: don't log package name
                                showBlockedAppNotification(foregroundApp)
                                lastNotificationTime = now
                            }
                        }
                    }
                } catch (_: IOException) {
                    break
                } catch (_: InterruptedException) {
                    break
                }
            }
            try {
                stream.close()
            } catch (ex: IOException) {
                // Log.w(TAG, "Error closing drain stream", ex) // Benign
            }
        }.apply {
            isDaemon = true
            start()
            Log.d(TAG, "Firewall VPN drain thread started")
        }
    }

    private fun showBlockedAppNotification(packageName: String) {
        val appName = try {
            // Optimization: Could cache this, but for now just running it on a background thread (which this is) is okay.
            // The drain thread is a background thread.
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        val allowIntent = Intent(this, FirewallNotificationReceiver::class.java).apply {
            action = FirewallNotificationReceiver.ACTION_ALLOW_APP
            putExtra(FirewallNotificationReceiver.EXTRA_PACKAGE_NAME, packageName)
        }
        val allowPendingIntent = PendingIntent.getBroadcast(
            this,
            packageName.hashCode(),
            allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, FIREWALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(getString(R.string.firewall_notification_blocked_title, appName))
            .setContentText(getString(R.string.firewall_notification_blocked_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action(
                    0,
                    getString(R.string.firewall_notification_action_allow),
                    allowPendingIntent
                )
            )
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(packageName.hashCode(), notification)
    }

    companion object {
        const val ACTION_START_OR_UPDATE = "com.silentport.silentport.firewall.START"
        const val ACTION_STOP = "com.silentport.silentport.firewall.STOP"
        const val EXTRA_BLOCKING = "extra_blocking"
        const val EXTRA_BLOCK_LIST = "extra_block_list"
        const val FIREWALL_CHANNEL_ID = "firewall_channel"
        private const val NOTIFICATION_ID = 1011
        private const val TAG = "VpnFirewallService"
        private const val VPN_ADDRESS = "10.100.0.2"
    }
}
