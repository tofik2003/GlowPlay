package com.glowplay.player

import com.glowplay.player.util.FileSizeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun bytesAndKb() {
        assertEquals("0 B", FileSizeFormatter.format(-3))
        assertEquals("512 B", FileSizeFormatter.format(512))
        assertEquals("1.0 KB", FileSizeFormatter.format(1024))
    }

    @Test
    fun mbAndGb() {
        assertEquals("1.5 MB", FileSizeFormatter.format((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", FileSizeFormatter.format(2L * 1024 * 1024 * 1024))
    }
}
