package com.glowplay.player.enhance

import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.playback.FilmFx

/**
 * A cinematic film look applies a full recipe in one tap: a color grade
 * (brightness/contrast/saturation/warmth) plus the film post-processing stack
 * (sharpen/vignette/grain). This is the GlowPlay equivalent of a LUT preset,
 * implemented on the existing single-matrix + GLSL pipeline so it stays live,
 * cheap and reversible.
 */
enum class FilmLook(val storageKey: String) {
    NONE("none"),
    NOIR("noir"),
    TEAL("teal"),
    FADE("fade"),
    VINTAGE("vintage");

    /**
     * The grade to apply, or null to leave the current grade untouched (NONE).
     * The film values are always applied, so NONE restores zero film FX.
     */
    data class Recipe(
        val grade: EnhanceSettings?,
        val film: FilmFx,
    )

    fun recipe(): Recipe = when (this) {
        NONE -> Recipe(
            grade = null,
            film = FilmFx(),
        )
        NOIR -> Recipe(
            grade = EnhanceSettings(
                brightness = -0.10f,
                contrast = 0.30f,
                saturation = -0.80f,
                warmth = -0.18f,
                glow = 0.20f,
                enabled = true,
            ),
            film = FilmFx(sharpen = 0.18f, vignette = 0.45f, grain = 0.45f),
        )
        TEAL -> Recipe(
            grade = EnhanceSettings(
                brightness = 0.02f,
                contrast = 0.24f,
                saturation = 0.24f,
                warmth = -0.30f,
                glow = 0.42f,
                enabled = true,
            ),
            film = FilmFx(sharpen = 0.12f, vignette = 0.25f, grain = 0.28f),
        )
        FADE -> Recipe(
            grade = EnhanceSettings(
                brightness = 0.07f,
                contrast = -0.12f,
                saturation = -0.22f,
                warmth = 0.10f,
                glow = 0.36f,
                enabled = true,
            ),
            film = FilmFx(sharpen = 0f, vignette = 0.30f, grain = 0.35f),
        )
        VINTAGE -> Recipe(
            grade = EnhanceSettings(
                brightness = -0.02f,
                contrast = 0.12f,
                saturation = -0.10f,
                warmth = 0.45f,
                glow = 0.40f,
                enabled = true,
            ),
            film = FilmFx(sharpen = 0f, vignette = 0.40f, grain = 0.50f),
        )
    }

    /** The film sliders implied by this look (used to reflect it in the UI). */
    fun filmFx(): FilmFx = recipe().film

    companion object {
        fun fromKey(key: String?): FilmLook =
            entries.firstOrNull { it.storageKey == key } ?: NONE
    }
}
