package com.glowplay.player

import com.glowplay.player.data.local.PlaybackStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStoreCodecTest {
    @Test
    fun roundTrip() {
        val encoded = PlaybackStore.serialize(mapOf("12" to 4400L, "content://v" to 99L))
        val parsed = PlaybackStore.parse(encoded)
        assertEquals(4400L, parsed["12"])
        assertEquals(99L, parsed["content://v"])
    }

    @Test
    fun ignoresCorruptLines() {
        val parsed = PlaybackStore.parse("badline\nkey|notanumber\n|9\nok|15")
        assertEquals(mapOf("ok" to 15L), parsed)
        assertTrue(PlaybackStore.parse("").isEmpty())
    }
}
