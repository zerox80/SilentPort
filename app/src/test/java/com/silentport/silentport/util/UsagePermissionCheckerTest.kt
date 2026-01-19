package com.silentport.silentport.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsagePermissionCheckerTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var appOpsManager: AppOpsManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.getSystemService(AppOpsManager::class.java) } returns appOpsManager
        
        val appInfo = mockk<ApplicationInfo>()
        appInfo.uid = 12345
        every { context.applicationInfo } returns appInfo
        every { context.packageName } returns "com.test.app"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isUsageAccessGranted returns true when MODE_ALLOWED`() {
        // Build.VERSION.SDK_INT is 0 in local tests, so it uses checkOpNoThrow (deprecated path)
        every { 
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                12345,
                "com.test.app"
            )
        } returns AppOpsManager.MODE_ALLOWED

        assertTrue(UsagePermissionChecker.isUsageAccessGranted(context))
    }

    @Test
    fun `isUsageAccessGranted returns false when MODE_IGNORED`() {
        every { 
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                12345,
                "com.test.app"
            )
        } returns AppOpsManager.MODE_IGNORED

        assertFalse(UsagePermissionChecker.isUsageAccessGranted(context))
    }
}
