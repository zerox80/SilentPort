package com.silentport.silentport.firewall

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppMonitor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getForegroundApp(): String? {
        val time = System.currentTimeMillis()
        // Look back 24 hours to ensure we catch the last move to foreground event
        val events = usageStatsManager.queryEvents(time - 24 * 60 * 60 * 1000, time)
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
