package com.silentport.silentport.domain

import com.silentport.silentport.data.local.TrackedAppEntity

data class UsageEvaluation(
    val updates: List<TrackedAppEntity>,
    val packagesToRemove: List<String>,
    val appsToNotify: List<TrackedAppEntity>,
    val appsForDisableRecommendation: List<TrackedAppEntity>
)
