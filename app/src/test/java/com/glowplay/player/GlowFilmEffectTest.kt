package com.glowplay.player

import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.GlowEffects
import com.glowplay.player.playback.FilmFx
import com.glowplay.player.playback.GlowFilmEffect
import com.glowplay.player.playback.GlowPlayerFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlowFilmEffectTest {

    @Test
    fun filmFxIdentityDetectsAllZero() {
        assertTrue(FilmFx().isIdentity)
        assertTrue(FilmFx(sharpen = 0f, vignette = 0f, grain = 0f).isIdentity)
        assertFalse(FilmFx(sharpen = 0.5f).isIdentity)
        assertFalse(FilmFx(vignette = 0.1f).isIdentity)
        assertFalse(FilmFx(grain = 0.2f).isIdentity)
    }

    @Test
    fun colorGradeOnlyProducesSingleEffect() {
        val commands = GlowEffects.commands(EnhancePreset.GLOW.settingsOr(EnhanceSettings()))
        val effects = GlowPlayerFactory.toMedia3Effects(commands)
        assertEquals(1, effects.size)
    }

    @Test
    fun filmFxAppendsSecondEffectAfterColorGrade() {
        val commands = GlowEffects.commands(EnhancePreset.GLOW.settingsOr(EnhanceSettings()))
        val effects = GlowPlayerFactory.toMedia3Effects(
            commands,
            FilmFx(sharpen = 0.5f, vignette = 0.2f, grain = 0.1f),
        )
        assertEquals(2, effects.size)
        assertTrue(effects[1] is GlowFilmEffect)
    }

    @Test
    fun noColorAndNoFilmProducesEmptyList() {
        assertTrue(GlowPlayerFactory.toMedia3Effects(emptyList(), FilmFx()).isEmpty())
    }

    @Test
    fun filmAloneProducesSingleFilmEffect() {
        val effects = GlowPlayerFactory.toMedia3Effects(emptyList(), FilmFx(sharpen = 1f))
        assertEquals(1, effects.size)
        assertTrue(effects[0] is GlowFilmEffect)
    }

    @Test
    fun glowFilmEffectIsNoOpReflectsParameters() {
        assertTrue(GlowFilmEffect(0f, 0f, 0f).isNoOp(1920, 1080))
        assertFalse(GlowFilmEffect(0f, 0.4f, 0f).isNoOp(1920, 1080))
        assertFalse(GlowFilmEffect(0.3f, 0f, 0f).isNoOp(1920, 1080))
    }
}
