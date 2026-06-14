package com.silentport.silentport.firewall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryUnblocksTest {

    @Test
    fun `encode and decode round-trip via validPackages`() {
        val entry = TemporaryUnblocks.encode("com.example.app", expiryMillis = 1_000L)
        assertEquals("com.example.app:1000", entry)
        assertEquals(setOf("com.example.app"), TemporaryUnblocks.validPackages(setOf(entry), now = 500L))
    }

    @Test
    fun `validPackages drops expired entries`() {
        val entries = setOf(
            TemporaryUnblocks.encode("a", 1_000L),
            TemporaryUnblocks.encode("b", 3_000L)
        )
        assertEquals(setOf("b"), TemporaryUnblocks.validPackages(entries, now = 2_000L))
    }

    @Test
    fun `validPackages ignores malformed entries`() {
        val entries = setOf("no-separator", "pkg:not-a-number", "pkg:1:2")
        assertTrue(TemporaryUnblocks.validPackages(entries, now = 0L).isEmpty())
    }

    @Test
    fun `nextExpiry returns earliest future expiry`() {
        val entries = setOf(
            TemporaryUnblocks.encode("a", 1_000L),
            TemporaryUnblocks.encode("b", 5_000L),
            TemporaryUnblocks.encode("c", 9_000L)
        )
        assertEquals(5_000L, TemporaryUnblocks.nextExpiry(entries, now = 2_000L))
    }

    @Test
    fun `nextExpiry is null when nothing is pending`() {
        val entries = setOf(TemporaryUnblocks.encode("a", 1_000L))
        assertNull(TemporaryUnblocks.nextExpiry(entries, now = 2_000L))
    }

    @Test
    fun `matches checks package prefix only`() {
        val entry = TemporaryUnblocks.encode("com.example.app", 1_000L)
        assertTrue(TemporaryUnblocks.matches(entry, "com.example.app"))
        assertFalse(TemporaryUnblocks.matches(entry, "com.example"))
        assertFalse(TemporaryUnblocks.matches(entry, "com.other.app"))
    }
}
