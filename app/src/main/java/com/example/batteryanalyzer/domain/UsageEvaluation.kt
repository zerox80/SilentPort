package com.example.batteryanalyzer.domain

import com.example.batteryanalyzer.data.local.TrackedAppEntity

data class UsageEvaluation(
    val updates: List<TrackedAppEntity>,
    val packagesToRemove: List<String>,
    val appsToNotify: List<TrackedAppEntity>,
    val appsForDisableRecommendation: List<TrackedAppEntity>
)
