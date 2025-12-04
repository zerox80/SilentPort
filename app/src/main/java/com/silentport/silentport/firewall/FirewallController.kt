package com.silentport.silentport.firewall

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.silentport.silentport.ui.state.FirewallUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

private const val AUTO_BLOCK_UNIQUE_WORK = "firewall_auto_block"
private val DEFAULT_ALLOW_DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(4)

class FirewallController(
    context: Context,
    private val preferences: FirewallPreferencesDataSource
) {

    private val appContext = context.applicationContext
    private val workManager: WorkManager = WorkManager.getInstance(appContext)
    private val stateMutex = Mutex()

    val state: Flow<FirewallUiState> = preferences.preferencesFlow.map { prefs ->
        FirewallUiState(
            isEnabled = prefs.isEnabled,
            isBlocking = prefs.isBlocking,
            reactivateAt = prefs.reactivateAt,
            blockedPackages = prefs.blockedPackages,
            whitelistedPackages = prefs.whitelistedPackages,
            temporaryUnblocks = prefs.temporaryUnblocks
        )
    }

    suspend fun enableFirewall(blockPackages: Set<String>? = null, allowDurationMillis: Long = DEFAULT_ALLOW_DURATION_MILLIS) = stateMutex.withLock {
        val packages = blockPackages ?: preferences.preferencesFlow.first().blockedPackages
        val reactivateAt = System.currentTimeMillis() + allowDurationMillis
        Log.i(TAG, "enableFirewall -> allowDuration=${allowDurationMillis} packages=${packages.size}")
        preferences.setState(
            isEnabled = true,
            isBlocking = false,
            reactivateAt = reactivateAt,
            blockedPackages = packages
        )
        scheduleAutoBlock(reactivateAt)
        startService(isBlocking = false, blockList = packages)
    }

    suspend fun allowForDuration(allowDurationMillis: Long, blockPackages: Set<String>? = null) = stateMutex.withLock {
        val packages = blockPackages ?: preferences.preferencesFlow.first().blockedPackages
        if (packages.isEmpty()) {
            Log.i(TAG, "allowForDuration -> no packages, delegating to setBlocking(false)")
            // We need to release lock before calling another suspended function that acquires it, or refactor.
            // setBlocking acquires lock. Re-entrant locks are not supported by Mutex.
            // Refactoring to internal method or just inlining logic.
            // Simplest is to just duplicate logic or extract internal helper.
            // Let's extract internal helper.
        }
        // Actually, let's just implement it directly here to avoid recursion issues with Mutex
        if (packages.isEmpty()) {
             Log.i(TAG, "allowForDuration -> no packages, disabling blocking")
             preferences.setState(
                isEnabled = true,
                isBlocking = false,
                reactivateAt = System.currentTimeMillis() + allowDurationMillis,
                blockedPackages = packages
            )
            scheduleAutoBlock(System.currentTimeMillis() + allowDurationMillis)
            startService(isBlocking = false, blockList = packages, includeBlockListWhenNotBlocking = true)
            return@withLock
        }

        Log.i(TAG, "allowForDuration -> allowDuration=${allowDurationMillis} keepBlocked=${packages.size}")
        preferences.setState(
            isEnabled = true,
            isBlocking = false,
            reactivateAt = System.currentTimeMillis() + allowDurationMillis,
            blockedPackages = packages
        )
        scheduleAutoBlock(System.currentTimeMillis() + allowDurationMillis)
        startService(isBlocking = false, blockList = packages)
    }

    suspend fun blockNow(blockPackages: Set<String>? = null) = stateMutex.withLock {
        val packages = blockPackages ?: preferences.preferencesFlow.first().blockedPackages
        Log.i(TAG, "blockNow -> packages=${packages.size}")
        preferences.setState(
            isEnabled = true,
            isBlocking = true,
            reactivateAt = null,
            blockedPackages = packages
        )
        cancelAutoBlock()
        startService(isBlocking = true, blockList = packages)
    }

    suspend fun applyManualBlockList(blockPackages: Set<String>) = stateMutex.withLock {
        val current = preferences.preferencesFlow.first()
        val shouldPersist = !current.isEnabled || current.isBlocking || current.reactivateAt != null || current.blockedPackages != blockPackages
        if (shouldPersist) {
            Log.i(TAG, "applyManualBlockList -> ${blockPackages.size} packages (was ${current.blockedPackages.size})")
            preferences.setState(
                isEnabled = true,
                isBlocking = false,
                reactivateAt = null,
                blockedPackages = blockPackages
            )
        } else {
            Log.d(TAG, "applyManualBlockList -> no state change required")
        }
        cancelAutoBlock()
        startService(isBlocking = false, blockList = blockPackages, includeBlockListWhenNotBlocking = true)
    }

    suspend fun disableFirewall() = stateMutex.withLock {
        Log.i(TAG, "disableFirewall")
        preferences.setState(isEnabled = false, isBlocking = false, reactivateAt = null, blockedPackages = emptySet())
        cancelAutoBlock()
        stopService()
    }

    suspend fun setBlocking(blocking: Boolean, reactivateAt: Long?, blockPackages: Set<String>? = null) = stateMutex.withLock {
        val packages = blockPackages ?: preferences.preferencesFlow.first().blockedPackages
        Log.i(TAG, "setBlocking -> blocking=$blocking reactivateAt=$reactivateAt packages=${packages.size}")
        preferences.setState(
            isEnabled = true,
            isBlocking = blocking,
            reactivateAt = reactivateAt,
            blockedPackages = packages
        )
        if (blocking) {
            cancelAutoBlock()
        } else if (reactivateAt != null) {
            scheduleAutoBlock(reactivateAt)
        }
        val includeBlockList = !blocking && reactivateAt == null && packages.isNotEmpty()
        startService(isBlocking = blocking, blockList = packages, includeBlockListWhenNotBlocking = includeBlockList)
    }

    suspend fun updateBlockedPackages(blockPackages: Set<String>) = stateMutex.withLock {
        val current = preferences.preferencesFlow.first()
        if (current.blockedPackages == blockPackages) {
            Log.d(TAG, "updateBlockedPackages -> no change (${blockPackages.size})")
            return@withLock
        }
        Log.i(TAG, "updateBlockedPackages -> ${blockPackages.size} packages (was ${current.blockedPackages.size})")

        // Bug fix 19: Filter out valid temporary unblocks to prevent race conditions
        val now = System.currentTimeMillis()
        val validUnblocks = current.temporaryUnblocks.mapNotNull { 
            val parts = it.split(":")
            if (parts.size == 2 && (parts[1].toLongOrNull() ?: 0L) > now) parts[0] else null
        }.toSet()
        
        val filteredPackages = blockPackages - validUnblocks

        preferences.setState(
            isEnabled = current.isEnabled,
            isBlocking = current.isBlocking,
            reactivateAt = current.reactivateAt,
            blockedPackages = filteredPackages
        )

        if (current.isEnabled) {
            val includeBlockList = (!current.isBlocking && current.reactivateAt == null && filteredPackages.isNotEmpty())
            startService(
                isBlocking = current.isBlocking,
                blockList = filteredPackages,
                includeBlockListWhenNotBlocking = includeBlockList
            )
        }
    }

    suspend fun updateWhitelistedPackages(whitelistedPackages: Set<String>) = stateMutex.withLock {
        preferences.setWhitelistedPackages(whitelistedPackages)
    }

    suspend fun temporarilyUnblock(packageName: String, durationMillis: Long = TimeUnit.MINUTES.toMillis(10)) = stateMutex.withLock {
        Log.i(TAG, "temporarilyUnblock -> $packageName for ${durationMillis}ms")
        preferences.addTemporaryUnblock(packageName, durationMillis)

        // Update blocked packages immediately by removing this one
        val current = preferences.preferencesFlow.first()
        val newBlocked = current.blockedPackages - packageName
        
        // Inline updateBlockedPackages logic to avoid re-entrancy
        if (current.blockedPackages != newBlocked) {
             preferences.setState(
                isEnabled = current.isEnabled,
                isBlocking = current.isBlocking,
                reactivateAt = current.reactivateAt,
                blockedPackages = newBlocked
            )
            if (current.isEnabled) {
                val includeBlockList = (!current.isBlocking && current.reactivateAt == null && newBlocked.isNotEmpty())
                startService(
                    isBlocking = current.isBlocking,
                    blockList = newBlocked,
                    includeBlockListWhenNotBlocking = includeBlockList
                )
            }
        }
    }

    private fun startService(
        isBlocking: Boolean,
        blockList: Set<String>,
        includeBlockListWhenNotBlocking: Boolean = false
    ) {
        Log.d(TAG, "startService -> blocking=$isBlocking blockList=${blockList.size}")
        val intent = Intent(appContext, VpnFirewallService::class.java).apply {
            action = VpnFirewallService.ACTION_START_OR_UPDATE
            putExtra(VpnFirewallService.EXTRA_BLOCKING, isBlocking)
            val payload = when {
                isBlocking && blockList.size < 500 -> ArrayList(blockList)
                includeBlockListWhenNotBlocking && blockList.size < 500 -> ArrayList(blockList)
                else -> arrayListOf()
            }
            putStringArrayListExtra(VpnFirewallService.EXTRA_BLOCK_LIST, payload)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    private fun stopService() {
        Log.d(TAG, "stopService")
        val intent = Intent(appContext, VpnFirewallService::class.java).apply {
            action = VpnFirewallService.ACTION_STOP
        }
        appContext.stopService(intent)
    }

    private fun scheduleAutoBlock(reactivateAt: Long) {
        val delay = reactivateAt - System.currentTimeMillis()
        Log.d(TAG, "scheduleAutoBlock -> reactivateAt=$reactivateAt delay=$delay")
        if (delay <= 0) {
            // Bug fix 16: Check if it's too stale (more than 1 hour ago)
            if (delay < -TimeUnit.HOURS.toMillis(1)) {
                Log.w(TAG, "scheduleAutoBlock -> reactivateAt is too stale ($delay ms), ignoring")
                return
            }
            Log.w(TAG, "scheduleAutoBlock -> delay <= 0, enqueuing immediate block")
            workManager.enqueueUniqueWork(
                AUTO_BLOCK_UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<FirewallAutoBlockWorker>().build()
            )
            return
        }
        val request = OneTimeWorkRequestBuilder<FirewallAutoBlockWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            AUTO_BLOCK_UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAutoBlock() {
        Log.d(TAG, "cancelAutoBlock")
        workManager.cancelUniqueWork(AUTO_BLOCK_UNIQUE_WORK)
    }

    companion object {
        private const val TAG = "FirewallController"
    }
}
