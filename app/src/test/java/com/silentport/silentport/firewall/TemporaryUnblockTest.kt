package com.silentport.silentport.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryUnblockTest {

    @Test
    fun `encode and decode round-trip`() {
        val entry = TemporaryUnblock("com.example.app", expiresAt = 1_000L).encode()
        assertEquals("com.example.app:1000", entry)
        val decoded = TemporaryUnblock.decode(entry)
        assertEquals(TemporaryUnblock("com.example.app", 1_000L), decoded)
    }

    @Test
    fun `decode rejects malformed entries`() {
        assertNull(TemporaryUnblock.decode("no-separator"))
        assertNull(TemporaryUnblock.decode("pkg:not-a-number"))
        assertNull(TemporaryUnblock.decode("pkg:1:2"))
    }

    @Test
    fun `isActiveAt reflects expiry`() {
        val unblock = TemporaryUnblock("a", expiresAt = 1_000L)
        assertTrue(unblock.isActiveAt(500L))
        assertFalse(unblock.isActiveAt(1_000L))
        assertFalse(unblock.isActiveAt(2_000L))
    }

    @Test
    fun `activePackages keeps only entries valid at now`() {
        val entries = setOf(
            TemporaryUnblock("a", 1_000L).encode(),
            TemporaryUnblock("b", 3_000L).encode()
        )
        assertEquals(setOf("b"), TemporaryUnblock.activePackages(entries, nowMillis = 2_000L))
    }

    @Test
    fun `activePackages ignores malformed entries`() {
        val entries = setOf("no-separator", "pkg:not-a-number", "pkg:1:2")
        assertTrue(TemporaryUnblock.activePackages(entries, nowMillis = 0L).isEmpty())
    }
}
