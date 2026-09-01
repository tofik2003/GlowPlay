package com.glowplay.player.data.model

data class EnhanceSettings(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val glow: Float = 0.45f,
    val enabled: Boolean = false,
) {
    fun clamped(): EnhanceSettings = copy(
        brightness = brightness.coerceIn(RANGE),
        contrast = contrast.coerceIn(RANGE),
        saturation = saturation.coerceIn(RANGE),
        warmth = warmth.coerceIn(RANGE),
        glow = glow.coerceIn(0f, 1f),
    )

    companion object {
        const val RANGE_MIN = -1f
        const val RANGE_MAX = 1f
        private val RANGE = RANGE_MIN..RANGE_MAX
        val Original = EnhanceSettings(enabled = false, glow = 0.25f)
    }
}
