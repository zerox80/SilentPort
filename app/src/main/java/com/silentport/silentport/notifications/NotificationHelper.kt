package com.silentport.silentport.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.silentport.silentport.R
import com.silentport.silentport.work.UsageSyncWorker

object NotificationHelper {

    fun showDisableReminderNotification(
        context: Context,
        appLabel: String,
        packageName: String,
        isRecommendation: Boolean,
        durationMillis: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return
        }

        val (titleRes, textRes) = if (isRecommendation) {
            R.string.notification_disable_recommendation_title to R.string.notification_disable_recommendation_text
        } else {
            R.string.notification_disable_title to R.string.notification_disable_text
        }

        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            intent,
            pendingIntentFlags
        )

        val durationLabel = com.silentport.silentport.ui.formatDuration(context, durationMillis)

        val builder = NotificationCompat.Builder(context, UsageSyncWorker.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(textRes, appLabel, durationLabel))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        manager.notify(packageName.hashCode(), builder.build())
    }

    private val pendingIntentFlags: Int
        get() {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }
            return flags
        }
}

