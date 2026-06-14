package com.silentport.silentport.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

/**
 * Thin wrapper around [PackageManager] lookups shared by the ViewModel and the traffic
 * sampler. Centralises the SDK-version branch for `getApplicationInfo` (previously copied
 * into both `resolveUid` and `isSystemApp`) and caches resolved UIDs.
 */
class PackageInspector(context: Context) {

    // Callers pass an application context, so query its PackageManager directly.
    private val packageManager: PackageManager = context.packageManager
    private val uidCache = ConcurrentHashMap<String, Int>()

    /** Drops cached UIDs; call after app installs/updates may have changed them. */
    fun clearCache() {
        uidCache.clear()
    }

    /** Resolves and caches the Linux UID for [packageName], or null if not installed. */
    fun resolveUid(packageName: String): Int? {
        uidCache[packageName]?.let { return it }
        return applicationInfoOrNull(packageName)?.uid?.also { uidCache[packageName] = it }
    }

    fun isSystemApp(packageName: String): Boolean {
        val info = applicationInfoOrNull(packageName) ?: return false
        return (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun applicationInfoOrNull(packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
