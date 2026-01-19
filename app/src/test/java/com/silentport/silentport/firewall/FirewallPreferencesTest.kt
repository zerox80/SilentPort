package com.silentport.silentport.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirewallPreferencesTest {

    @Test
    fun `default values are correct`() {
        val prefs = FirewallPreferences(
            isEnabled = false,
            isBlocking = false,
            reactivateAt = null,
            blockedPackages = emptySet(),
            whitelistedPackages = emptySet()
        )
        
        assertFalse(prefs.isEnabled)
        assertFalse(prefs.isBlocking)
        assertNull(prefs.reactivateAt)
        assertTrue(prefs.blockedPackages.isEmpty())
        assertTrue(prefs.whitelistedPackages.isEmpty())
        assertTrue(prefs.temporaryUnblocks.isEmpty())
    }

    @Test
    fun `copy with new values works correctly`() {
        val original = FirewallPreferences(
            isEnabled = false,
            isBlocking = false,
            reactivateAt = null,
            blockedPackages = emptySet(),
            whitelistedPackages = emptySet()
        )
        
        val modified = original.copy(
            isEnabled = true,
            isBlocking = true,
            blockedPackages = setOf("com.example.app")
        )
        
        assertTrue(modified.isEnabled)
        assertTrue(modified.isBlocking)
        assertEquals(1, modified.blockedPackages.size)
        assertTrue(modified.blockedPackages.contains("com.example.app"))
        
        // Original should be unchanged
        assertFalse(original.isEnabled)
        assertFalse(original.isBlocking)
    }

    @Test
    fun `reactivateAt timestamp is stored correctly`() {
        val reactivateTime = System.currentTimeMillis() + 60000
        
        val prefs = FirewallPreferences(
            isEnabled = true,
            isBlocking = false,
            reactivateAt = reactivateTime,
            blockedPackages = emptySet(),
            whitelistedPackages = emptySet()
        )
        
        assertEquals(reactivateTime, prefs.reactivateAt)
    }

    @Test
    fun `temporary unblocks are parsed correctly`() {
        val now = System.currentTimeMillis()
        val expiry = now + 60000
        
        val prefs = FirewallPreferences(
            isEnabled = true,
            isBlocking = true,
            reactivateAt = null,
            blockedPackages = setOf("com.blocked.app"),
            whitelistedPackages = emptySet(),
            temporaryUnblocks = setOf("com.temp.app:$expiry")
        )
        
        assertEquals(1, prefs.temporaryUnblocks.size)
        val entry = prefs.temporaryUnblocks.first()
        assertTrue(entry.startsWith("com.temp.app:"))
    }

    @Test
    fun `whitelisted packages are stored correctly`() {
        val prefs = FirewallPreferences(
            isEnabled = true,
            isBlocking = true,
            reactivateAt = null,
            blockedPackages = emptySet(),
            whitelistedPackages = setOf("com.trusted.app1", "com.trusted.app2")
        )
        
        assertEquals(2, prefs.whitelistedPackages.size)
        assertTrue(prefs.whitelistedPackages.contains("com.trusted.app1"))
        assertTrue(prefs.whitelistedPackages.contains("com.trusted.app2"))
    }
}
