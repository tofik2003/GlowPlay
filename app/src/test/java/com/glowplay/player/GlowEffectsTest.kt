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
    fun pipelineIsAlwaysInitialised() {
        // The effect chain must never be empty — this is what allows live
        // preset changes mid-playback (the media3 pipeline stays warm).
        val disabled = GlowEffects.commands(EnhanceSettings.Original)
        assertEquals(GlowEffectCommand.Type.entries.size, disabled.size)
        assertTrue(disabled.all { it.value == 0f })
    }

    @Test
    fun disabledPresetIsIdentity() {
        assertTrue(GlowEffects.isIdentity(EnhanceSettings.Original))
        assertTrue(GlowEffects.isIdentity(EnhancePreset.OFF.settingsOr(EnhanceSettings())))
        assertTrue(GlowEffects.activeCommands(EnhanceSettings.Original).isEmpty())
    }

    @Test
    fun glowPresetEnablesColorPipeline() {
        val commands = GlowEffects.activeCommands(EnhancePreset.GLOW.settingsOr(EnhanceSettings()))
        assertFalse(commands.isEmpty())
        assertTrue(commands.any { it.type == GlowEffectCommand.Type.SATURATION })
        assertTrue(commands.any { it.type == GlowEffectCommand.Type.CONTRAST })
    }

    @Test
    fun sunsetPresetUsesHueAndTint() {
        val commands = GlowEffects.activeCommands(EnhancePreset.SUNSET.settingsOr(EnhanceSettings()))
        assertTrue(commands.any { it.type == GlowEffectCommand.Type.HUE })
        assertTrue(commands.any { it.type == GlowEffectCommand.Type.TINT })
    }

    @Test
    fun monoPresetDesaturates() {
        val commands = GlowEffects.activeCommands(EnhancePreset.MONO.settingsOr(EnhanceSettings()))
        val saturation = commands.first { it.type == GlowEffectCommand.Type.SATURATION }
        assertTrue(saturation.value < -0.5f)
    }

    @Test
    fun warmthScalesRedUpAndBlueDown() {
        assertEquals(1.18f, GlowEffects.redScale(1f), 0.0001f)
        assertEquals(0.82f, GlowEffects.blueScale(1f), 0.0001f)
        assertEquals(0.82f, GlowEffects.redScale(-1f), 0.0001f)
        assertEquals(1.18f, GlowEffects.blueScale(-1f), 0.0001f)
    }

    @Test
    fun tintPushesGreenMagentaAxis() {
        assertEquals(0.86f, GlowEffects.tintGreenScale(1f), 0.0001f)
        assertEquals(1.14f, GlowEffects.tintGreenScale(-1f), 0.0001f)
        assertEquals(1.05f, GlowEffects.tintRedBlueScale(1f), 0.0001f)
    }

    @Test
    fun hueMapsToDegrees() {
        assertEquals(48f, GlowEffects.hueDegrees(1f), 0.0001f)
        assertEquals(-24f, GlowEffects.hueDegrees(-0.5f), 0.0001f)
        assertEquals(0f, GlowEffects.hueDegrees(0f), 0.0001f)
    }

    @Test
    fun saturationMapsToPercent() {
        assertEquals(100f, GlowEffects.saturationPercent(1f), 0.0001f)
        assertEquals(-50f, GlowEffects.saturationPercent(-0.5f), 0.0001f)
    }

    @Test
    fun clampRejectsOutOfRange() {
        val clamped = EnhanceSettings(brightness = 4f, hue = -7f, tint = 3f, glow = 9f, enabled = true).clamped()
        assertEquals(1f, clamped.brightness, 0f)
        assertEquals(-1f, clamped.hue, 0f)
        assertEquals(1f, clamped.tint, 0f)
        assertEquals(1f, clamped.glow, 0f)
    }

    @Test
    fun tinyValuesAreIdentity() {
        val settings = EnhanceSettings(brightness = 0.001f, enabled = true)
        assertTrue(GlowEffects.isIdentity(settings))
    }
}
