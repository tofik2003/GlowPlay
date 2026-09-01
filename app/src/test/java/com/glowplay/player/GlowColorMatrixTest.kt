package com.glowplay.player

import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.GlowEffectCommand
import com.glowplay.player.enhance.GlowEffects
import com.glowplay.player.playback.GlowColorMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GlowColorMatrixTest {

    private fun matrix(vararg commands: GlowEffectCommand): FloatArray {
        val m = GlowColorMatrix.fromCommands(commands.toList())
        assertNotNull(m)
        return m!!.getMatrix(0L, false)
    }

    @Test
    fun noCommandsReturnsNull() {
        assertNull(GlowColorMatrix.fromCommands(emptyList()))
    }

    @Test
    fun brightnessAddsOffsetInFourthColumn() {
        val m = matrix(GlowEffectCommand(GlowEffectCommand.Type.BRIGHTNESS, 0.5f))
        assertEquals(1f, m[0], 0f) // red scale untouched
        assertEquals(0.5f, m[12], 0.0001f) // additive red offset
        assertEquals(0.5f, m[13], 0.0001f) // additive green offset
        assertEquals(0.5f, m[14], 0.0001f) // additive blue offset
    }

    @Test
    fun contrastScalesAroundMidGray() {
        val m = matrix(GlowEffectCommand(GlowEffectCommand.Type.CONTRAST, 0.5f))
        val factor = 1.5f / 0.5001f
        assertEquals(factor, m[0], 0.0001f)
        assertEquals((1f - factor) * 0.5f, m[12], 0.0001f)
    }

    @Test
    fun warmthScalesRedUpBlueDownAndKeepsGreen() {
        val m = matrix(GlowEffectCommand(GlowEffectCommand.Type.WARMTH, 1f))
        assertEquals(1.18f, m[0], 0.0001f) // red
        assertEquals(1f, m[5], 0f) // green unchanged
        assertEquals(0.82f, m[10], 0.0001f) // blue
    }

    @Test
    fun saturationIsLumaWeighted() {
        val m = matrix(GlowEffectCommand(GlowEffectCommand.Type.SATURATION, 1f))
        assertEquals(0.2126f * -1f + 2f, m[0], 0.0001f) // 1.7874
        assertEquals(0.2126f * -1f, m[1], 0.0001f) // -0.2126
        assertEquals(0.0722f * -1f + 2f, m[10], 0.0001f) // 1.9278
    }

    @Test
    fun glowPresetProducesNonIdentityMatrix() {
        val settings = EnhancePreset.GLOW.settingsOr(EnhanceSettings())
        val commands = GlowEffects.commands(settings)
        val m = GlowColorMatrix.fromCommands(commands)
        assertNotNull(m)
        val array = m!!.getMatrix(0L, false)
        var isIdentity = true
        for (i in array.indices) {
            val expected = if (i % 5 == 0) 1f else 0f
            if (array[i] != expected) isIdentity = false
        }
        assertEquals(false, isIdentity)
    }
}
