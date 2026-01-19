package com.silentport.silentport.util

import android.content.Context
import android.text.format.DateUtils
import com.silentport.silentport.R
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UsageTextFormatterTest {

    @MockK
    lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(DateUtils::class)
        every { context.getString(R.string.last_used_never) } returns "Never used"
        every { context.getString(R.string.last_used_format, *anyVararg()) } answers { 
            val varargs = args[1] as Array<*>
            "Last used: ${varargs[0]}" 
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `formatLastUsed returns never string when null`() {
        val result = UsageTextFormatter.formatLastUsed(context, null)
        assertEquals("Never used", result)
    }

    @Test
    fun `formatLastUsed returns formatted string when not null`() {
        val now = System.currentTimeMillis()
        val lastUsed = now - 60000 
        
        every { 
            DateUtils.getRelativeTimeSpanString(any(), any(), any(), any()) 
        } returns "1 minute ago"
        
        val result = UsageTextFormatter.formatLastUsed(context, lastUsed)
        assertEquals("Last used: 1 minute ago", result)
    }
}
