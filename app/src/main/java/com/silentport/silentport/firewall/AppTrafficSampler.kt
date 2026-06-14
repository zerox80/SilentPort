package com.silentport.silentport.firewall

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.RemoteException
import android.util.Log
import com.silentport.silentport.util.PackageInspector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Samples per-app network usage over a sliding time window.
 *
 * Prefers the system [NetworkStatsManager] (accurate, historical) and falls back to
 * cumulative [TrafficStats] counters sampled over time when that is unavailable. All of
 * this used to live inside `MainViewModel`; it is extracted here so the ViewModel only has
 * to wire the result into UI state.
 *
 * Sampling is serialised by [mutex]; callers may invoke [sample] and [clear] concurrently.
 */
class AppTrafficSampler(
    context: Context,
    private val packageInspector: PackageInspector
) {

    private val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)
    private val trafficSnapshots = ConcurrentHashMap<String, Long>()
    private val trafficHistory = ConcurrentHashMap<String, ArrayDeque<Pair<Long, Long>>>()
    private val mutex = Mutex()

    /** Clears all accumulated history. Safe to call from any thread. */
    suspend fun clear() = mutex.withLock {
        trafficHistory.clear()
        trafficSnapshots.clear()
    }

    /**
     * Returns total bytes (rx + tx) per package over the last [windowMillis], for the apps
     * in [trackedPackages]. An empty input yields an empty map and resets internal history.
     */
    suspend fun sample(
        trackedPackages: List<String>,
        now: Long,
        windowMillis: Long
    ): Map<String, Long> = mutex.withLock {
        if (trackedPackages.isEmpty()) {
            trafficHistory.clear()
            trafficSnapshots.clear()
            return@withLock emptyMap()
        }

        val trackedPackageSet = trackedPackages.toSet()
        val packageUidPairs = trackedPackages.mapNotNull { packageName ->
            packageInspector.resolveUid(packageName)?.let { uid -> packageName to uid }
        }

        val networkStatsSums = collectViaNetworkStats(packageUidPairs, now - windowMillis, now)
        if (networkStatsSums != null) {
            trafficHistory.clear()
            trafficSnapshots.clear()
            return@withLock networkStatsSums
        }

        // Fallback: accumulate deltas of cumulative per-uid counters into a sliding window.
        val fallbackSums = mutableMapOf<String, Long>()
        for ((packageName, uid) in packageUidPairs) {
            collectFromSnapshots(packageName, uid, now, windowMillis)?.let { sum ->
                fallbackSums[packageName] = sum
            }
        }
        pruneUntracked(trackedPackageSet)
        fallbackSums
    }

    private fun pruneUntracked(trackedPackageSet: Set<String>) {
        val iterator = trafficHistory.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key !in trackedPackageSet) {
                iterator.remove()
                trafficSnapshots.remove(key)
            }
        }
    }

    private fun collectFromSnapshots(
        packageName: String,
        uid: Int,
        now: Long,
        windowMillis: Long
    ): Long? {
        val rxBytes = TrafficStats.getUidRxBytes(uid)
        val txBytes = TrafficStats.getUidTxBytes(uid)
        if (rxBytes < 0 || txBytes < 0) return null

        val totalBytes = rxBytes + txBytes
        val previousTotal = trafficSnapshots.put(packageName, totalBytes)
        val delta = if (previousTotal == null || totalBytes < previousTotal) 0L else totalBytes - previousTotal

        val history = trafficHistory.getOrPut(packageName) { ArrayDeque() }
        if (delta > 0) {
            history.addLast(now to delta)
        }
        while (history.isNotEmpty() && now - history.first().first > windowMillis) {
            history.removeFirst()
        }
        while (history.size > MAX_HISTORY_SIZE) {
            history.removeFirst()
        }
        return history.sumOf { it.second }
    }

    private fun collectViaNetworkStats(
        packageUidPairs: List<Pair<String, Int>>,
        start: Long,
        end: Long
    ): Map<String, Long>? {
        val manager = networkStatsManager ?: return null
        if (packageUidPairs.isEmpty()) return emptyMap()

        val safeStart = start.coerceAtLeast(0L)
        val uidToPackage = packageUidPairs.associate { (packageName, uid) -> uid to packageName }
        val totals = mutableMapOf<String, Long>()
        val bucket = NetworkStats.Bucket()

        val networkTypes = buildList {
            add(ConnectivityManager.TYPE_WIFI)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(ConnectivityManager.TYPE_ETHERNET)
            }
        }

        for (networkType in networkTypes) {
            try {
                manager.querySummary(networkType, null, safeStart, end).use { stats ->
                    while (stats.hasNextBucket()) {
                        stats.getNextBucket(bucket)
                        val packageName = uidToPackage[bucket.uid] ?: continue
                        val bytes = bucket.rxBytes + bucket.txBytes
                        if (bytes <= 0) continue
                        totals[packageName] = (totals[packageName] ?: 0L) + bytes
                    }
                }
            } catch (error: SecurityException) {
                Log.w(TAG, "Network stats permission missing", error)
                return null
            } catch (error: RemoteException) {
                Log.w(TAG, "Unable to query network stats", error)
            } catch (error: RuntimeException) {
                Log.e(TAG, "Skipping network type $networkType due to error", error)
            }
        }

        return totals
    }

    companion object {
        private const val TAG = "AppTrafficSampler"
        private const val MAX_HISTORY_SIZE = 100
    }
}
