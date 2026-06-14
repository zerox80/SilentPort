package com.silentport.silentport

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silentport.silentport.data.local.AppUsageStatus
import com.silentport.silentport.data.repository.UsageRepository
import com.silentport.silentport.di.AppContainer
import com.silentport.silentport.domain.ApplicationManager
import com.silentport.silentport.domain.BlockListCalculator
import com.silentport.silentport.domain.UsageAnalyzer
import com.silentport.silentport.firewall.AppTrafficSampler
import com.silentport.silentport.firewall.FirewallAllowlist
import com.silentport.silentport.firewall.FirewallController
import com.silentport.silentport.firewall.TemporaryUnblock
import com.silentport.silentport.model.AppUsageInfo
import com.silentport.silentport.settings.SettingsPreferencesDataSource
import com.silentport.silentport.ui.state.AppHomeState
import com.silentport.silentport.util.PackageInspector
import com.silentport.silentport.util.UsagePermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.buildList

class MainViewModel(
    private val appContext: Context,
    private val usageAnalyzer: UsageAnalyzer,
    private val applicationManager: ApplicationManager,
    private val usageRepository: UsageRepository,
    private val firewallController: FirewallController,
    private val settingsPreferences: SettingsPreferencesDataSource
) : ViewModel() {

    private val firewallAllowlist = FirewallAllowlist.PACKAGES

    private val _uiState = MutableStateFlow(AppHomeState(hardcodedAllowlist = firewallAllowlist))
    val uiState: StateFlow<AppHomeState> = _uiState.asStateFlow()

    private val packageInspector = PackageInspector(appContext)
    private val trafficSampler = AppTrafficSampler(appContext, packageInspector)

    private var metricsJob: Job? = null
    private var manualUnblockSchedulerJob: Job? = null
    private var temporaryUnblockJob: Job? = null

    private val trafficWindowMillisRef = AtomicLong(TimeUnit.MINUTES.toMillis(10))
    private val trafficSampleIntervalMillis = TimeUnit.SECONDS.toMillis(30)
    private val blockThresholdMillisRef = AtomicLong(TimeUnit.DAYS.toMillis(4))

    private val manualUnblockCooldown = ConcurrentHashMap<String, Long>()

    private var subscriptionsStarted = false

    init {
        observeSettings()
        subscribeToData()
        subscribeToFirewall()
        refreshUsage()
    }

    private fun subscribeToFirewall() {
        viewModelScope.launch {
            firewallController.state.collect { state ->
                _uiState.value = _uiState.value.copy(
                    firewallState = state,
                    firewallBlockedPackages = state.blockedPackages,
                    whitelistedPackages = state.whitelistedPackages,
                    firewallTemporaryUnblocks = state.temporaryUnblocks
                )
                scheduleTemporaryUnblockSync()
            }
        }
    }

    private fun scheduleTemporaryUnblockSync() {
        val now = System.currentTimeMillis()
        val nextExpiry = _uiState.value.firewallTemporaryUnblocks
            .mapNotNull { TemporaryUnblock.decode(it)?.expiresAt }
            .filter { it > now }
            .minOrNull()

        if (nextExpiry == null) {
            temporaryUnblockJob?.cancel()
            temporaryUnblockJob = null
            return
        }

        val delayMillis = (nextExpiry - now).coerceAtLeast(0)
        temporaryUnblockJob?.cancel()
        temporaryUnblockJob = viewModelScope.launch {
            Log.d(TAG, "Scheduled temporary unblock sync in ${delayMillis}ms")
            delay(delayMillis)
            Log.i(TAG, "Temporary unblock expired -> resyncing firewall")
            syncFirewallBlockList()
        }
    }

    /**
     * Computes the current block list and applies it. In manual mode the list is pushed as a manual
     * block list; otherwise [automatic] decides how the firewall controller consumes it.
     */
    private fun applyBlockList(
        action: String,
        automatic: suspend (state: AppHomeState, blockList: Set<String>) -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val blockList = computeBlockList(state)
            Log.i(TAG, "$action requested. manual=${state.manualFirewallUnblock} blockCount=${blockList.size}")
            if (state.manualFirewallUnblock) {
                firewallController.applyManualBlockList(blockList)
            } else {
                automatic(state, blockList)
            }
        }
    }

    fun enableFirewall() = applyBlockList("enableFirewall") { state, blockList ->
        firewallController.enableFirewall(blockList, state.allowDurationMillis)
    }

    fun blockNow() = applyBlockList("blockNow") { _, blockList ->
        firewallController.blockNow(blockList)
    }

    fun disableFirewall() {
        viewModelScope.launch {
            Log.i(TAG, "disableFirewall requested")
            firewallController.disableFirewall()
        }
    }

    fun allowForConfiguredDuration() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.manualFirewallUnblock) {
                Log.i(TAG, "Manual firewall mode active; skipping allowForConfiguredDuration")
                return@launch
            }
            val blockList = computeBlockList()
            Log.i(TAG, "allowForDuration requested for ${state.allowDurationMillis}ms. blockCount=${blockList.size}")
            firewallController.allowForDuration(state.allowDurationMillis, blockList)
        }
    }

    fun updateAllowDuration(durationMillis: Long) {
        viewModelScope.launch {
            settingsPreferences.setAllowDurationMillis(durationMillis)
        }
    }

    fun setMetricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setMetricsEnabled(enabled)
            if (!enabled) {
                stopMetricsMonitor()
            }
        }
    }

    fun setManualFirewallUnblock(enabled: Boolean) {
        viewModelScope.launch {
            Log.i(TAG, "setManualFirewallUnblock -> $enabled")
            settingsPreferences.setManualFirewallUnblock(enabled)
            if (enabled) {
                val state = _uiState.value.copy(manualFirewallUnblock = true)
                val blockList = computeBlockList(state)
                Log.i(TAG, "Manual mode enabled. Applying manual block list with ${blockList.size} packages")
                firewallController.applyManualBlockList(blockList)
                scheduleManualUnblockSync()
            } else {
                manualUnblockSchedulerJob?.cancel()
                manualUnblockSchedulerJob = null
                manualUnblockCooldown.clear()
                syncFirewallBlockList()
            }
        }
    }

    fun setHideSystemApps(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setHideSystemApps(enabled)
        }
    }

    fun addToManualSystemApps(packageName: String) {
        viewModelScope.launch {
            val current = _uiState.value.manualSystemApps.toMutableSet()
            if (current.add(packageName)) {
                settingsPreferences.setManualSystemApps(current)
            }
        }
    }

    fun removeFromManualSystemApps(packageName: String) {
        viewModelScope.launch {
            val current = _uiState.value.manualSystemApps.toMutableSet()
            if (current.remove(packageName)) {
                settingsPreferences.setManualSystemApps(current)
            }
        }
    }

    fun refreshMetricsNow() {
        if (!_uiState.value.metricsEnabled) return
        viewModelScope.launch(Dispatchers.IO) {
            sampleAppTraffic()
        }
    }

    fun manualUnblockPackage(packageName: String) {
        viewModelScope.launch {
            Log.i(TAG, "manualUnblockPackage -> $packageName")
            val now = System.currentTimeMillis()
            val scheduledReblockAt = _uiState.value.firewallState.reactivateAt
            val allowDurationMillis = _uiState.value.allowDurationMillis
            val candidateExpiries = buildList {
                scheduledReblockAt?.takeIf { it > now }?.let { add(it) }
                if (allowDurationMillis > 0) {
                    add(now + allowDurationMillis)
                }
            }
            val cooldownUntil = candidateExpiries.minOrNull() ?: (now + blockThresholdMillisRef.get())
            Log.d(TAG, "manualUnblockPackage cooldown -> pkg=$packageName until=$cooldownUntil candidates=${candidateExpiries.size}")
            manualUnblockCooldown[packageName] = cooldownUntil
            val current = _uiState.value.firewallBlockedPackages.toMutableSet()
            if (current.remove(packageName)) {
                Log.d(TAG, "Package removed from manual block set: $packageName")
                firewallController.updateBlockedPackages(current)
            }
            scheduleManualUnblockSync()
        }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            val hasPermission = UsagePermissionChecker.isUsageAccessGranted(appContext)
            // Invalidate the UID cache so app updates/reinstalls don't resolve to stale UIDs.
            packageInspector.clearCache()
            _uiState.value = _uiState.value.copy(usagePermissionGranted = hasPermission, isLoading = true)

            if (!hasPermission) {
                _uiState.value = AppHomeState(usagePermissionGranted = false, isLoading = false)
                return@launch
            }

            val result = runCatching {
                withContext(Dispatchers.IO) { usageAnalyzer.evaluateUsage() }
            }

            result.onSuccess { evaluation ->
                usageRepository.applyEvaluation(evaluation)
                syncFirewallBlockList()
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to refresh usage", throwable)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun addToWhitelist(packageName: String) {
        viewModelScope.launch {
            val current = _uiState.value.whitelistedPackages.toMutableSet()
            if (current.add(packageName)) {
                firewallController.updateWhitelistedPackages(current)
                syncFirewallBlockList()
            }
        }
    }

    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch {
            val current = _uiState.value.whitelistedPackages.toMutableSet()
            if (current.remove(packageName)) {
                firewallController.updateWhitelistedPackages(current)
                syncFirewallBlockList()
            }
        }
    }

    private fun subscribeToData() {
        if (subscriptionsStarted) return
        subscriptionsStarted = true

        val hideSystemAppsFlow = settingsPreferences.preferencesFlow
            .map { it.hideSystemApps }
            .distinctUntilChanged()

        launchStatusCollector(AppUsageStatus.RECENT, hideSystemAppsFlow) { recent ->
            _uiState.update { it.copy(recentApps = recent, isLoading = false) }
        }
        launchStatusCollector(AppUsageStatus.RARE, hideSystemAppsFlow) { rare ->
            _uiState.update { it.copy(rareApps = rare, isLoading = false) }
        }
    }

    private fun launchStatusCollector(
        status: AppUsageStatus,
        hideSystemAppsFlow: Flow<Boolean>,
        onUpdate: (List<AppUsageInfo>) -> Unit
    ) {
        viewModelScope.launch {
            combine(
                usageRepository.observeStatus(status),
                hideSystemAppsFlow,
                settingsPreferences.preferencesFlow.map { it.manualSystemApps }.distinctUntilChanged()
            ) { apps, hideSystem, manualSystemApps ->
                if (hideSystem) {
                    apps.filter { !it.isSystemApp && it.packageName !in manualSystemApps }
                } else {
                    apps
                }
            }.collect {
                onUpdate(it)
                syncFirewallBlockList()
            }
        }
    }

    private fun computeBlockList(state: AppHomeState = _uiState.value): Set<String> {
        val now = System.currentTimeMillis()
        val thresholdMillis = blockThresholdMillisRef.get().coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
        val validTemporaryUnblocks = TemporaryUnblock.activePackages(state.firewallTemporaryUnblocks, now)

        val result = BlockListCalculator.compute(
            BlockListCalculator.Inputs(
                rareApps = state.rareApps,
                now = now,
                thresholdMillis = thresholdMillis,
                manualMode = state.manualFirewallUnblock,
                currentBlocked = state.firewallBlockedPackages,
                manualCooldown = manualUnblockCooldown,
                whitelist = state.whitelistedPackages,
                hardcodedAllowlist = firewallAllowlist,
                selfPackage = appContext.packageName,
                hideSystemApps = state.hideSystemApps,
                manualSystemApps = state.manualSystemApps,
                validTemporaryUnblocks = validTemporaryUnblocks
            ),
            isSystemApp = packageInspector::isSystemApp
        )
        Log.d(TAG, "computeBlockList manual=${state.manualFirewallUnblock} -> ${result.size} packages")
        return result
    }

    private fun scheduleManualUnblockSync() {
        val nextExpiry = manualUnblockCooldown.values.minOrNull()
        if (nextExpiry == null) {
            manualUnblockSchedulerJob?.cancel()
            manualUnblockSchedulerJob = null
            return
        }

        val delayMillis = (nextExpiry - System.currentTimeMillis()).coerceAtLeast(0)
        manualUnblockSchedulerJob?.cancel()
        manualUnblockSchedulerJob = viewModelScope.launch {
            Log.d(TAG, "manualUnblock scheduler waiting ${delayMillis}ms for next expiry")
            delay(delayMillis)
            Log.i(TAG, "manualUnblock scheduler expired -> resyncing firewall")
            syncFirewallBlockList()
        }
    }

    private suspend fun syncFirewallBlockList() {
        // Drop expired cooldowns before recomputing so they no longer suppress blocking.
        val now = System.currentTimeMillis()
        manualUnblockCooldown.values.removeAll { it <= now }

        val desired = computeBlockList()
        val current = _uiState.value.firewallBlockedPackages
        if (desired != current) {
            Log.i(TAG, "syncFirewallBlockList updating firewall. desired=${desired.size}, current=${current.size}")
            firewallController.updateBlockedPackages(desired)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsPreferences.preferencesFlow.collect { prefs ->
                val previousState = _uiState.value
                val allowDurationChanged = prefs.allowDurationMillis != previousState.allowDurationMillis
                val previousMetricsEnabled = previousState.metricsEnabled
                val manualModeChanged = prefs.manualFirewallUnblock != previousState.manualFirewallUnblock

                val policyUpdated = usageAnalyzer.updatePolicyThresholds(prefs.allowDurationMillis)

                if (allowDurationChanged || policyUpdated) {
                    val window = prefs.allowDurationMillis.coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
                    trafficWindowMillisRef.set(window)
                    blockThresholdMillisRef.set(window)
                }

                _uiState.value = previousState.copy(
                    allowDurationMillis = prefs.allowDurationMillis,
                    metricsEnabled = prefs.metricsEnabled,
                    manualFirewallUnblock = prefs.manualFirewallUnblock,
                    hideSystemApps = prefs.hideSystemApps,
                    manualSystemApps = prefs.manualSystemApps
                )

                when {
                    prefs.metricsEnabled && !previousMetricsEnabled -> startMetricsMonitor()
                    !prefs.metricsEnabled && previousMetricsEnabled -> stopMetricsMonitor()
                }

                if (allowDurationChanged || policyUpdated) {
                    refreshUsage()
                }

                if (manualModeChanged) {
                    syncFirewallBlockList()
                }
            }
        }
    }

    private fun startMetricsMonitor() {
        if (metricsJob != null) return
        metricsJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                sampleAppTraffic()
                delay(trafficSampleIntervalMillis)
            }
        }
    }

    private fun stopMetricsMonitor() {
        val job = metricsJob
        metricsJob = null
        job?.cancel()
        // Clear traffic state under the mutex on a background coroutine so we never block the caller
        // (which may be the main thread) and never race with an in-flight sampleAppTraffic().
        viewModelScope.launch(Dispatchers.IO) {
            trafficSampler.clear()
        }
        _uiState.update { state ->
            state.copy(appTraffic = emptyMap(), metricsLastSampleAt = null)
        }
    }

    private suspend fun sampleAppTraffic() {
        val trackedPackages = (_uiState.value.recentApps + _uiState.value.rareApps)
            .map { it.packageName }
            .distinct()
        val now = System.currentTimeMillis()
        val sums = trafficSampler.sample(trackedPackages, now, trafficWindowMillisRef.get())
        _uiState.update { state ->
            state.copy(appTraffic = sums, metricsLastSampleAt = now)
        }
    }

    companion object {
        private const val TAG = "MainViewModel"

        fun Factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(
                        appContext = container.appContext,
                        usageAnalyzer = container.usageAnalyzer,
                        applicationManager = container.applicationManager,
                        usageRepository = container.usageRepository,
                        firewallController = container.firewallController,
                        settingsPreferences = container.settingsPreferences
                    ) as T
                }
            }
        }
    }
}
