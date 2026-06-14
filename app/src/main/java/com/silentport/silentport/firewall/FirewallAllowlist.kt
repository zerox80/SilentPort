package com.silentport.silentport.firewall

/**
 * Packages that are never auto-blocked by the firewall, regardless of usage.
 *
 * These are apps where losing connectivity would be unacceptable (e.g. banking and
 * 2FA apps). Kept here rather than inline in the ViewModel so the list is discoverable.
 */
object FirewallAllowlist {

    val PACKAGES: Set<String> = setOf(
        "com.google.android.youtube",
        // Sparkassen-Apps
        "com.starfinanz.mobile.android.pushtan",
        "de.starfinanz.smob.android.sfinanzstatus", // Sparkasse
        "de.starfinanz.smob.android.sfinanzstatus.tablet",
        "biz.first_financial.bk01_2fa" // BK Secure
    )
}
