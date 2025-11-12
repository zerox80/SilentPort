package com.privacyguard.privacyguard.domain

import com.privacyguard.privacyguard.data.local.TrackedAppEntity

data class UsageEvaluation(
    val updates: List<TrackedAppEntity>,
    val packagesToRemove: List<String>,
    val appsToNotify: List<TrackedAppEntity>,
    val appsForDisableRecommendation: List<TrackedAppEntity>
)
