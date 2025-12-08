package com.silentport.silentport.model

import com.silentport.silentport.data.local.AppUsageStatus

data class AppUsageInfo(
    val packageName: String,
    val appLabel: String,
    val lastUsedAt: Long?,
    val status: AppUsageStatus,
    val isDisabled: Boolean,
    val scheduledDisableAt: Long?,
    val notifiedAt: Long?,
    val isSystemApp: Boolean = false
)

