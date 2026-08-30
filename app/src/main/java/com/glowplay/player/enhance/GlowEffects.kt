package com.glowplay.player.enhance

import com.glowplay.player.data.model.EnhanceSettings
import kotlin.math.abs

data class GlowEffectCommand(
    val type: Type,
    val value: Float,
) {
    enum class Type {
        BRIGHTNESS,
        CONTRAST,
        SATURATION,
        WARMTH,
    }
}

object GlowEffects {
    private const val EPS = 0.004f

    fun commands(settings: EnhanceSettings): List<GlowEffectCommand> {
        if (!settings.enabled) return emptyList()
        val s = settings.clamped()
        val out = ArrayList<GlowEffectCommand>(4)
        if (abs(s.brightness) > EPS) {
            out += GlowEffectCommand(GlowEffectCommand.Type.BRIGHTNESS, s.brightness)
        }
        if (abs(s.contrast) > EPS) {
            out += GlowEffectCommand(GlowEffectCommand.Type.CONTRAST, s.contrast)
        }
        if (abs(s.saturation) > EPS) {
            out += GlowEffectCommand(GlowEffectCommand.Type.SATURATION, s.saturation)
        }
        if (abs(s.warmth) > EPS) {
            out += GlowEffectCommand(GlowEffectCommand.Type.WARMTH, s.warmth)
        }
        return out
    }

    fun isIdentity(settings: EnhanceSettings): Boolean = commands(settings).isEmpty()

    fun redScale(warmth: Float): Float = (1f + warmth.coerceIn(-1f, 1f) * 0.18f)

    fun blueScale(warmth: Float): Float = (1f - warmth.coerceIn(-1f, 1f) * 0.18f)

    fun saturationPercent(saturation: Float): Float = saturation.coerceIn(-1f, 1f) * 100f
}
