package com.example.batteryanalyzer.domain

/**
 * Encapsulates rules that decide whether a package should be tracked or receive disable/archive
 * recommendations. This keeps critical apps (for example banking or authentication) out of the
 * automation pipeline so the UI can present more trustworthy suggestions.
 */
class UsagePolicy(
    private val allowList: Set<String>? = null,
    private val denyList: Set<String> = DEFAULT_DENY_LIST,
    private val denyPrefixes: Set<String> = DEFAULT_DENY_PREFIXES
) {

    /**
     * Returns true when the package should not be tracked or targeted for automated actions.
     */
    fun shouldSkip(packageName: String): Boolean {
        allowList?.let { explicitAllow ->
            if (packageName in explicitAllow) return false
        }
        if (packageName in denyList) return true
        if (denyPrefixes.any { prefix -> packageName.startsWith(prefix) }) return true
        return false
    }

    companion object {
        private val DEFAULT_DENY_LIST = setOf(
            "com.google.android.apps.authenticator2",
            "proton.android.authenticator",
            "com.starfinanz.mobile.android.pushtan",
            "com.starfinanz.mobile.android.sparkasseplus",
            "com.google.android.apps.walletnfcrel",
            "com.google.android.apps.wallet",
            "com.google.android.apps.security.securityhub",
            "com.google.android.apps.work.clouddpc"
        )

        private val DEFAULT_DENY_PREFIXES = setOf(
            "com.starfinanz.",
            "com.bank",
            "ch.threema",
            "de.starface",
            "org.telegram",
            "com.google.android.safety",
            "cz.mobilesoft.appblock"
        )
    }
}
