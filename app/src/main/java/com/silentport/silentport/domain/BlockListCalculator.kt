package com.silentport.silentport.domain

import com.silentport.silentport.model.AppUsageInfo

/**
 * Pure computation of the set of packages the firewall should block.
 *
 * This used to live inside `MainViewModel` as two near-identical branches (manual vs.
 * automatic mode). The two branches only differ in their starting set; every other
 * filter is shared, so they are unified here behind a single sequence of exclusions.
 *
 * Kept free of Android dependencies so it can be unit-tested directly. The only platform
 * concern, "is this package a system app", is supplied by the [isSystemApp] predicate.
 */
object BlockListCalculator {

    data class Inputs(
        /** Apps currently classified as rarely used. */
        val rareApps: List<AppUsageInfo>,
        val now: Long,
        /** A rare app counts as blockable when unused for at least this long. */
        val thresholdMillis: Long,
        val manualMode: Boolean,
        /** Packages already blocked; the starting set in manual mode. */
        val currentBlocked: Set<String>,
        /** Per-package manual-unblock cooldown expiry (epoch millis). */
        val manualCooldown: Map<String, Long>,
        val whitelist: Set<String>,
        val hardcodedAllowlist: Set<String>,
        val selfPackage: String,
        val hideSystemApps: Boolean,
        val manualSystemApps: Set<String>,
        /** Packages with a currently-valid temporary unblock. */
        val validTemporaryUnblocks: Set<String>
    )

    fun compute(inputs: Inputs, isSystemApp: (String) -> Boolean): Set<String> = with(inputs) {
        val threshold: Long = now - thresholdMillis
        val rarePackages = rareApps
            .filter { info -> info.lastUsedAt == null || info.lastUsedAt <= threshold }
            .map { it.packageName }

        val base = if (manualMode) {
            // Keep what is already blocked, and add freshly-rare packages that are not
            // currently within their manual-unblock cooldown window.
            val additions = rarePackages.filter { pkg ->
                manualCooldown[pkg]?.let { expiry -> expiry <= now } ?: true
            }
            currentBlocked + additions
        } else {
            rarePackages.toSet()
        }

        return base.asSequence()
            .filterNot { it == selfPackage }
            .filterNot { it in hardcodedAllowlist }
            .filterNot { it in whitelist }
            .filterNot { it in validTemporaryUnblocks }
            .filterNot { hideSystemApps && (isSystemApp(it) || it in manualSystemApps) }
            .toSet()
    }
}
