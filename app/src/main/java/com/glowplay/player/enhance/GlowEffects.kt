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
        HUE,
        TINT,
    }
}

/**
 * Pure mapping layer between user-facing enhance settings and the GPU effect
 * pipeline.
 *
 * IMPORTANT: [commands] always emits the FULL command set (identity values are
 * kept as 0). The media3 effect pipeline only picks up effects reliably when
 * the pipeline is initialised before playback starts, so the player keeps a
 * stable, always-present effect chain and we only mutate the values. This is
 * what makes live preset switching work mid-playback.
 */
object GlowEffects {
    private const val EPS = 0.004f

    /** Full effect chain for the given settings. Disabled settings map to identity values. */
    fun commands(settings: EnhanceSettings): List<GlowEffectCommand> {
        val s = if (settings.enabled) settings.clamped() else EnhanceSettings.Original.copy(enabled = false)
        val active = settings.enabled
        fun v(value: Float) = if (active) value else 0f
        return listOf(
            GlowEffectCommand(GlowEffectCommand.Type.BRIGHTNESS, v(s.brightness)),
            GlowEffectCommand(GlowEffectCommand.Type.CONTRAST, v(s.contrast)),
            GlowEffectCommand(GlowEffectCommand.Type.SATURATION, v(s.saturation)),
            GlowEffectCommand(GlowEffectCommand.Type.WARMTH, v(s.warmth)),
            GlowEffectCommand(GlowEffectCommand.Type.HUE, v(s.hue)),
            GlowEffectCommand(GlowEffectCommand.Type.TINT, v(s.tint)),
        )
    }

    /** Commands that actually change pixels (used for badges / tests). */
    fun activeCommands(settings: EnhanceSettings): List<GlowEffectCommand> =
        commands(settings).filter { abs(it.value) > EPS }

    fun isIdentity(settings: EnhanceSettings): Boolean = activeCommands(settings).isEmpty()

    /** Warmth: shift white balance by scaling red up and blue down (or inverse). */
    fun redScale(warmth: Float): Float = (1f + warmth.coerceIn(-1f, 1f) * 0.18f)

    fun blueScale(warmth: Float): Float = (1f - warmth.coerceIn(-1f, 1f) * 0.18f)

    /** Tint: green <-> magenta axis. Positive tint pushes magenta (less green). */
    fun tintGreenScale(tint: Float): Float = (1f - tint.coerceIn(-1f, 1f) * 0.14f)

    fun tintRedBlueScale(tint: Float): Float = (1f + tint.coerceIn(-1f, 1f) * 0.05f)

    fun saturationPercent(saturation: Float): Float = saturation.coerceIn(-1f, 1f) * 100f

    /** Hue rotation in degrees for the HSL shader, mapped to a tasteful ±48° span. */
    fun hueDegrees(hue: Float): Float = hue.coerceIn(-1f, 1f) * 48f
}
