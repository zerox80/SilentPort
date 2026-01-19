package com.silentport.silentport

import android.content.Context
import com.silentport.silentport.data.local.AppUsageStatus
import com.silentport.silentport.data.repository.UsageRepository
import com.silentport.silentport.domain.ApplicationManager
import com.silentport.silentport.domain.UsageAnalyzer
import com.silentport.silentport.domain.UsageEvaluation
import com.silentport.silentport.firewall.FirewallController
import com.silentport.silentport.ui.state.FirewallUiState
import com.silentport.silentport.settings.AppSettingsPreferences
import com.silentport.silentport.settings.SettingsPreferencesDataSource
import com.silentport.silentport.util.UsagePermissionChecker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var usageAnalyzer: UsageAnalyzer

    @MockK
    lateinit var applicationManager: ApplicationManager

    @MockK
    lateinit var usageRepository: UsageRepository

    @MockK
    lateinit var firewallController: FirewallController

    @MockK
    lateinit var settingsPreferences: SettingsPreferencesDataSource

    private lateinit var viewModel: MainViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Mock UsagePermissionChecker static
        mockkObject(UsagePermissionChecker)
        every { UsagePermissionChecker.isUsageAccessGranted(any()) } returns true
        
        // Mock Settings flow
        every { settingsPreferences.preferencesFlow } returns flowOf(
            AppSettingsPreferences(
                allowDurationMillis = TimeUnit.MINUTES.toMillis(10),
                metricsEnabled = false,
                manualFirewallUnblock = false,
                hideSystemApps = false,
                manualSystemApps = emptySet()
            )
        )
        
        // Mock Firewall state
        every { firewallController.state } returns MutableStateFlow(FirewallUiState())
        
        // Mock UsageRepository flows
        every { usageRepository.observeStatus(AppUsageStatus.RECENT) } returns flowOf(emptyList())
        every { usageRepository.observeStatus(AppUsageStatus.RARE) } returns flowOf(emptyList())
        
        // Mock UsageAnalyzer
        coEvery { usageAnalyzer.evaluateUsage() } returns UsageEvaluation(
            updates = emptyList(),
            packagesToRemove = emptyList(),
            appsToNotify = emptyList(),
            appsForDisableRecommendation = emptyList()
        )
        every { usageAnalyzer.updatePolicyThresholds(any()) } returns false
        
        // Mock Repo apply
        coEvery { usageRepository.applyEvaluation(any()) } returns Unit
        
        // Mock Context calls in init (getPackageManager etc)
        every { context.packageManager } returns mockk()
        every { context.getSystemService(any<Class<Any>>()) } returns null // NetworkStatsManager
        every { context.packageName } returns "com.silentport.silentport"
        
        // Mock Firewall updateBlockedPackages (called during init via syncFirewallBlockList)
        coEvery { firewallController.updateBlockedPackages(any()) } returns Unit
        coEvery { firewallController.updateWhitelistedPackages(any()) } returns Unit
        
        // Mock settings update methods
        coEvery { settingsPreferences.setAllowDurationMillis(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = MainViewModel(
            appContext = context,
            usageAnalyzer = usageAnalyzer,
            applicationManager = applicationManager,
            usageRepository = usageRepository,
            firewallController = firewallController,
            settingsPreferences = settingsPreferences
        )
    }

    @Test
    fun `init refreshes usage and subscribes to data`() = runTest {
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { usageAnalyzer.evaluateUsage() }
        coVerify { usageRepository.observeStatus(AppUsageStatus.RECENT) }
        coVerify { firewallController.state }
    }

    @Test
    fun `enableFirewall triggers controller`() = runTest {
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { firewallController.enableFirewall(any(), any()) } returns Unit
        
        viewModel.enableFirewall()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { firewallController.enableFirewall(any(), any()) }
    }

    @Test
    fun `disableFirewall triggers controller`() = runTest {
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { firewallController.disableFirewall() } returns Unit
        
        viewModel.disableFirewall()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { firewallController.disableFirewall() }
    }
    
    @Test
    fun `updateAllowDuration updates settings`() = runTest {
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val newDuration = 5000L
        coEvery { settingsPreferences.setAllowDurationMillis(newDuration) } returns Unit
        
        viewModel.updateAllowDuration(newDuration)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { settingsPreferences.setAllowDurationMillis(newDuration) }
    }
    
    @Test
    fun `addToWhitelist updates controller`() = runTest {
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val pkg = "com.example.app"
        coEvery { firewallController.updateWhitelistedPackages(any()) } returns Unit
        
        viewModel.addToWhitelist(pkg)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { firewallController.updateWhitelistedPackages(match { it.contains(pkg) }) }
    }
}
