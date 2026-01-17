package com.silentport.silentport.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.silentport.silentport.SilentPortApp
import com.silentport.silentport.util.UsagePermissionChecker

import com.silentport.silentport.notifications.NotificationHelper

class UsageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext.applicationContext as SilentPortApp
        val container = app.container
        val context = container.appContext

        if (!UsagePermissionChecker.isUsageAccessGranted(context)) {
            // Bug fix: Do not retry indefinitely if permission is missing. It requires user action.
            return Result.failure()
        }

        return runCatching {
            val evaluation = container.usageAnalyzer.evaluateUsage()
            container.usageRepository.applyEvaluation(evaluation)

            val usagePolicy = container.usagePolicy
            
            evaluation.appsToNotify.forEach { entity ->
                NotificationHelper.showDisableReminderNotification(
                    context,
                    entity.appLabel,
                    entity.packageName,
                    isRecommendation = false,
                    durationMillis = usagePolicy.warningThresholdMillis
                )
            }
            
            evaluation.appsForDisableRecommendation.forEach { entity ->
                NotificationHelper.showDisableReminderNotification(
                    context,
                    entity.appLabel,
                    entity.packageName,
                    isRecommendation = true,
                    durationMillis = usagePolicy.disableThresholdMillis
                )
            }

            Result.success()
        }.getOrElse { throwable ->
            Log.e(TAG, "Usage sync failed", throwable)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "usage_sync_worker"
        const val NOTIFICATION_CHANNEL_ID = "usage_status_channel"
        private const val TAG = "UsageSyncWorker"
    }
}
