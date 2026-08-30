package com.glowplay.player

import com.glowplay.player.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun zeroAndNegativeBecomeZero() {
        assertEquals("0:00", TimeFormatter.formatMs(0))
        assertEquals("0:00", TimeFormatter.formatMs(-12))
    }

    @Test
    fun minutesAndSeconds() {
        assertEquals("0:01", TimeFormatter.formatMs(1_000))
        assertEquals("1:01", TimeFormatter.formatMs(61_000))
        assertEquals("10:00", TimeFormatter.formatMs(600_000))
    }

    @Test
    fun hours() {
        assertEquals("1:00:00", TimeFormatter.formatMs(3_600_000))
        assertEquals("1:02:03", TimeFormatter.formatMs(3_723_000))
    }

    @Test
    fun progressIsClamped() {
        assertEquals(0f, TimeFormatter.progress(10, 0), 0.0001f)
        assertEquals(0.5f, TimeFormatter.progress(50, 100), 0.0001f)
        assertEquals(1f, TimeFormatter.progress(200, 100), 0.0001f)
    }
}
