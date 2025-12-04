package com.silentport.silentport.firewall

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.silentport.silentport.util.UsagePermissionChecker

class ForegroundAppMonitor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getForegroundApp(): String? {
        if (!UsagePermissionChecker.isUsageAccessGranted(context)) {
            return null
        }
        val time = System.currentTimeMillis()
        // Look back 5 minutes instead of 24 hours. We check frequently so we don't need a long history.
        val events = usageStatsManager.queryEvents(time - 5 * 60 * 1000, time)
        var lastPackage: String? = null
        var lastTimeStamp = 0L
        val event = UsageEvents.Event()
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > lastTimeStamp) {
                    lastPackage = event.packageName
                    lastTimeStamp = event.timeStamp
                }
            }
        }
        return lastPackage
    }
}
