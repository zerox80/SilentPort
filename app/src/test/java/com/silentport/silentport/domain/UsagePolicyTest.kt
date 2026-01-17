package com.silentport.silentport.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class UsagePolicyTest {

    @Test
    fun `default values are set correctly`() {
        val policy = UsagePolicy()
        // Default values from UsagePolicy companion object hardcoded for verification
        // modifying these in the class should break this test, which is intended.
        // DEFAULT_RECENT_THRESHOLD = 2 days
        // DEFAULT_WARNING_THRESHOLD = 3 days
        // DEFAULT_DISABLE_THRESHOLD = 4 days
        
        assertEquals(TimeUnit.DAYS.toMillis(2), policy.recentThresholdMillis)
        assertEquals(TimeUnit.DAYS.toMillis(3), policy.warningThresholdMillis)
        assertEquals(TimeUnit.DAYS.toMillis(4), policy.disableThresholdMillis)
    }

    @Test
    fun `custom values are set correctly via constructor`() {
        val recent = 1000L
        val warning = 2000L
        val disable = 3000L
        val policy = UsagePolicy(recent, warning, disable)

        assertEquals(recent, policy.recentThresholdMillis)
        assertEquals(warning, policy.warningThresholdMillis)
        assertEquals(disable, policy.disableThresholdMillis)
    }

    @Test
    fun `shouldSkip returns false by default`() {
        val policy = UsagePolicy()
        assertFalse(policy.shouldSkip("com.example.app"))
    }

    @Test
    fun `updateThresholds updates values correctly`() {
        val policy = UsagePolicy()
        val newAllowDuration = TimeUnit.DAYS.toMillis(10)
        
        val changed = policy.updateThresholds(newAllowDuration)

        assertTrue("Should return true when values change", changed)
        
        // Logic from UsagePolicy:
        // newDisable = sanitized (10 days)
        // newWarning = sanitized * 3 / 4 (7.5 days)
        // newRecent = sanitized (10 days)
        
        assertEquals(newAllowDuration, policy.disableThresholdMillis)
        assertEquals(newAllowDuration * 3 / 4, policy.warningThresholdMillis)
        assertEquals(newAllowDuration, policy.recentThresholdMillis)
    }

    @Test
    fun `updateThresholds respects minimum threshold`() {
        val policy = UsagePolicy()
        val tinyDuration = 1L // Very small value
        val minThreshold = TimeUnit.MINUTES.toMillis(1) // From companion object

        val changed = policy.updateThresholds(tinyDuration)
        
        assertTrue(changed)
        assertEquals(minThreshold, policy.disableThresholdMillis)
        assertEquals(minThreshold, policy.recentThresholdMillis)
        // newWarning = sanitized * 3/4. If sanitized is minThreshold, then minThreshold * 3/4 might be < minThreshold.
        // But the code says: (sanitized * 3 / 4).coerceAtLeast(MIN_THRESHOLD)
        // So it should be MIN_THRESHOLD
        assertEquals(minThreshold, policy.warningThresholdMillis)
    }

    @Test
    fun `updateThresholds returns false if values don't change`() {
        val policy = UsagePolicy()
        val initialDisable = policy.disableThresholdMillis
        
        // Set to same value (assuming logic holds, 4 days is default)
        // default disable is 4 days.
        // updateThresholds(4 days) -> 
        // newDisable = 4 days
        // newRecent = 4 days (default was 2 days! So it WILL change)
        
        // Let's first stabilize it
        policy.updateThresholds(initialDisable)
        
        // Now call again with same value
        val changed = policy.updateThresholds(initialDisable)
        assertFalse("Should return false when values don't change", changed)
    }
}
