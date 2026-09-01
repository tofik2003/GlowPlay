package com.glowplay.player

import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.GlowEffectCommand
import com.glowplay.player.enhance.GlowEffects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlowEffectsTest {
    @Test
    fun disabledPresetEmitsNoCommands() {
        assertTrue(GlowEffects.commands(EnhanceSettings.Original).isEmpty())
        assertTrue(GlowEffects.isIdentity(EnhancePreset.OFF.settingsOr(EnhanceSettings())))
    }

    @Test
    fun glowPresetEnablesColorPipeline() {
        val commands = GlowEffects.commands(EnhancePreset.GLOW.settingsOr(EnhanceSettings()))
        assertFalse(commands.isEmpty())
        assertTrue(commands.any { it.type == GlowEffectCommand.Type.SATURATION })
        assertTrue(commands.any { it.type == GlowEffectCommand.Type.CONTRAST })
    }

    @Test
    fun warmthScalesRedUpAndBlueDown() {
        assertEquals(1.18f, GlowEffects.redScale(1f), 0.0001f)
        assertEquals(0.82f, GlowEffects.blueScale(1f), 0.0001f)
        assertEquals(0.82f, GlowEffects.redScale(-1f), 0.0001f)
        assertEquals(1.18f, GlowEffects.blueScale(-1f), 0.0001f)
    }

    @Test
    fun saturationMapsToPercent() {
        assertEquals(100f, GlowEffects.saturationPercent(1f), 0.0001f)
        assertEquals(-50f, GlowEffects.saturationPercent(-0.5f), 0.0001f)
    }

    @Test
    fun clampRejectsOutOfRange() {
        val clamped = EnhanceSettings(brightness = 4f, glow = 9f, enabled = true).clamped()
        assertEquals(1f, clamped.brightness, 0f)
        assertEquals(1f, clamped.glow, 0f)
    }

    @Test
    fun tinyValuesAreIdentity() {
        val settings = EnhanceSettings(brightness = 0.001f, enabled = true)
        assertTrue(GlowEffects.isIdentity(settings))
    }
}
