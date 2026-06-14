package com.silentport.silentport.domain

import com.silentport.silentport.data.local.AppUsageStatus
import com.silentport.silentport.model.AppUsageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockListCalculatorTest {

    private val now = 1_000_000L
    private val threshold = 10_000L

    private fun rareApp(pkg: String, lastUsedAt: Long?) = AppUsageInfo(
        packageName = pkg,
        appLabel = pkg,
        lastUsedAt = lastUsedAt,
        status = AppUsageStatus.RARE,
        isDisabled = false,
        scheduledDisableAt = null,
        notifiedAt = null
    )

    private fun inputs(
        rareApps: List<AppUsageInfo>,
        manualMode: Boolean = false,
        currentBlocked: Set<String> = emptySet(),
        manualCooldown: Map<String, Long> = emptyMap(),
        whitelist: Set<String> = emptySet(),
        hardcodedAllowlist: Set<String> = emptySet(),
        selfPackage: String = "com.silentport.silentport",
        hideSystemApps: Boolean = false,
        manualSystemApps: Set<String> = emptySet(),
        validTemporaryUnblocks: Set<String> = emptySet()
    ) = BlockListCalculator.Inputs(
        rareApps = rareApps,
        now = now,
        thresholdMillis = threshold,
        manualMode = manualMode,
        currentBlocked = currentBlocked,
        manualCooldown = manualCooldown,
        whitelist = whitelist,
        hardcodedAllowlist = hardcodedAllowlist,
        selfPackage = selfPackage,
        hideSystemApps = hideSystemApps,
        manualSystemApps = manualSystemApps,
        validTemporaryUnblocks = validTemporaryUnblocks
    )

    @Test
    fun `blocks rare apps unused past the threshold`() {
        val result = BlockListCalculator.compute(
            inputs(rareApps = listOf(rareApp("a", lastUsedAt = now - threshold - 1))),
            isSystemApp = { false }
        )
        assertEquals(setOf("a"), result)
    }

    @Test
    fun `keeps apps used within the threshold`() {
        val result = BlockListCalculator.compute(
            inputs(rareApps = listOf(rareApp("a", lastUsedAt = now - 1))),
            isSystemApp = { false }
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `never-used apps (null lastUsed) are blocked`() {
        val result = BlockListCalculator.compute(
            inputs(rareApps = listOf(rareApp("a", lastUsedAt = null))),
            isSystemApp = { false }
        )
        assertEquals(setOf("a"), result)
    }

    @Test
    fun `excludes self, allowlist, whitelist and temporary unblocks`() {
        val rare = listOf("self", "allow", "white", "temp", "a").map { rareApp(it, lastUsedAt = null) }
        val result = BlockListCalculator.compute(
            inputs(
                rareApps = rare,
                selfPackage = "self",
                hardcodedAllowlist = setOf("allow"),
                whitelist = setOf("white"),
                validTemporaryUnblocks = setOf("temp")
            ),
            isSystemApp = { false }
        )
        assertEquals(setOf("a"), result)
    }

    @Test
    fun `hideSystemApps removes system and manual system apps`() {
        val rare = listOf("sys", "manualSys", "a").map { rareApp(it, lastUsedAt = null) }
        val result = BlockListCalculator.compute(
            inputs(
                rareApps = rare,
                hideSystemApps = true,
                manualSystemApps = setOf("manualSys")
            ),
            isSystemApp = { it == "sys" }
        )
        assertEquals(setOf("a"), result)
    }

    @Test
    fun `manual mode keeps current blocked and adds non-cooldown rare apps`() {
        val result = BlockListCalculator.compute(
            inputs(
                rareApps = listOf(rareApp("new", lastUsedAt = null), rareApp("cooling", lastUsedAt = null)),
                manualMode = true,
                currentBlocked = setOf("existing"),
                manualCooldown = mapOf("cooling" to now + 5_000L)
            ),
            isSystemApp = { false }
        )
        assertEquals(setOf("existing", "new"), result)
        assertFalse(result.contains("cooling"))
    }

    @Test
    fun `manual mode adds rare app once its cooldown expired`() {
        val result = BlockListCalculator.compute(
            inputs(
                rareApps = listOf(rareApp("warm", lastUsedAt = null)),
                manualMode = true,
                manualCooldown = mapOf("warm" to now - 1)
            ),
            isSystemApp = { false }
        )
        assertEquals(setOf("warm"), result)
    }
}
