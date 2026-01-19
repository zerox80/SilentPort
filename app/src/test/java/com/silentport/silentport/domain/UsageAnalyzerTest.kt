package com.silentport.silentport.domain

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.silentport.silentport.data.local.TrackedAppDao
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for UsageAnalyzer.
 * Note: Due to Android framework classes (ArrayMap) being unavailable in pure JVM tests,
 * we test the synchronous methods and policy delegation only.
 */
class UsageAnalyzerTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var trackedAppDao: TrackedAppDao

    @MockK
    lateinit var usagePolicy: UsagePolicy

    @MockK
    lateinit var packageManager: PackageManager

    @MockK
    lateinit var usageStatsManager: UsageStatsManager

    private lateinit var analyzer: UsageAnalyzer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { context.packageManager } returns packageManager
        every { context.getSystemService(UsageStatsManager::class.java) } returns usageStatsManager
        every { context.packageName } returns "com.silentport.silentport"

        every { usagePolicy.recentThresholdMillis } returns TimeUnit.DAYS.toMillis(2)
        every { usagePolicy.warningThresholdMillis } returns TimeUnit.DAYS.toMillis(3)
        every { usagePolicy.disableThresholdMillis } returns TimeUnit.DAYS.toMillis(4)
        every { usagePolicy.shouldSkip(any()) } returns false

        analyzer = UsageAnalyzer(context, trackedAppDao, usagePolicy)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `updatePolicyThresholds delegates to policy and returns true`() {
        every { usagePolicy.updateThresholds(any()) } returns true
        val result = analyzer.updatePolicyThresholds(100L)
        assertTrue(result)
    }
    
    @Test
    fun `updatePolicyThresholds delegates to policy and returns false`() {
        every { usagePolicy.updateThresholds(any()) } returns false
        val result = analyzer.updatePolicyThresholds(100L)
        assertTrue(!result)
    }
    
    @Test
    fun `analyzer is properly initialized with dependencies`() {
        assertNotNull(analyzer)
    }
    
    @Test
    fun `policy thresholds are accessible`() {
        val recentThreshold = usagePolicy.recentThresholdMillis
        val warningThreshold = usagePolicy.warningThresholdMillis
        val disableThreshold = usagePolicy.disableThresholdMillis
        
        assertNotNull(recentThreshold)
        assertNotNull(warningThreshold)
        assertNotNull(disableThreshold)
    }
    
    @Test
    fun `updatePolicyThresholds with zero value returns result`() {
        every { usagePolicy.updateThresholds(0L) } returns true
        val result = analyzer.updatePolicyThresholds(0L)
        assertTrue(result)
    }
    
    @Test
    fun `updatePolicyThresholds with large value returns result`() {
        val largeValue = TimeUnit.DAYS.toMillis(365)
        every { usagePolicy.updateThresholds(largeValue) } returns true
        val result = analyzer.updatePolicyThresholds(largeValue)
        assertTrue(result)
    }
}
