package com.silentport.silentport.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

import com.silentport.silentport.data.local.AppUsageStatus
import com.silentport.silentport.data.local.TrackedAppDao
import com.silentport.silentport.data.local.TrackedAppEntity
import com.silentport.silentport.domain.UsageEvaluation
import com.silentport.silentport.model.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UsageRepository(
    private val context: Context,
    private val trackedAppDao: TrackedAppDao
) {

    fun observeStatus(status: AppUsageStatus): Flow<List<AppUsageInfo>> {
        return trackedAppDao.observeByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun applyEvaluation(evaluation: UsageEvaluation) {
        withContext(Dispatchers.IO) {
            if (evaluation.updates.isNotEmpty()) {
                trackedAppDao.upsertAll(evaluation.updates)
            }
            if (evaluation.packagesToRemove.isNotEmpty()) {
                trackedAppDao.deleteAll(evaluation.packagesToRemove)
            }
        }
    }

    private fun TrackedAppEntity.toDomain(): AppUsageInfo {
        val isSystem = runCatching {
             val pm = context.packageManager
             val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                 pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
             } else {
                 pm.getApplicationInfo(packageName, 0)
             }
             (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)

        return AppUsageInfo(
            packageName = packageName,
            appLabel = appLabel,
            lastUsedAt = lastUsedAt.takeIf { it > 0 },
            status = status,
            isDisabled = isDisabled,
            scheduledDisableAt = scheduledDisableAt,
            notifiedAt = notifiedAt,
            isSystemApp = isSystem
        )
    }
}
