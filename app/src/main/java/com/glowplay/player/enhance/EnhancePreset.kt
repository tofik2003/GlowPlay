package com.glowplay.player.enhance

import com.glowplay.player.data.model.EnhanceSettings

enum class EnhancePreset(val storageKey: String) {
    OFF("off"),
    GLOW("glow"),
    CINEMA("cinema"),
    VIVID("vivid"),
    NIGHT("night"),
    CRYSTAL("crystal"),
    WARM("warm"),
    COOL("cool"),
    CUSTOM("custom");

    fun settingsOr(custom: EnhanceSettings): EnhanceSettings = when (this) {
        OFF -> EnhanceSettings.Original
        GLOW -> EnhanceSettings(
            brightness = 0.06f,
            contrast = 0.14f,
            saturation = 0.22f,
            warmth = 0.06f,
            glow = 0.85f,
            enabled = true,
        )
        CINEMA -> EnhanceSettings(
            brightness = -0.03f,
            contrast = 0.20f,
            saturation = 0.08f,
            warmth = 0.14f,
            glow = 0.40f,
            enabled = true,
        )
        VIVID -> EnhanceSettings(
            brightness = 0.05f,
            contrast = 0.16f,
            saturation = 0.38f,
            warmth = 0.04f,
            glow = 0.70f,
            enabled = true,
        )
        NIGHT -> EnhanceSettings(
            brightness = 0.18f,
            contrast = 0.10f,
            saturation = -0.06f,
            warmth = -0.10f,
            glow = 0.30f,
            enabled = true,
        )
        CRYSTAL -> EnhanceSettings(
            brightness = 0.04f,
            contrast = 0.24f,
            saturation = 0.06f,
            warmth = 0.00f,
            glow = 0.50f,
            enabled = true,
        )
        WARM -> EnhanceSettings(
            brightness = 0.03f,
            contrast = 0.08f,
            saturation = 0.12f,
            warmth = 0.36f,
            glow = 0.55f,
            enabled = true,
        )
        COOL -> EnhanceSettings(
            brightness = 0.04f,
            contrast = 0.10f,
            saturation = 0.10f,
            warmth = -0.32f,
            glow = 0.60f,
            enabled = true,
        )
        CUSTOM -> custom.clamped().copy(enabled = true)
    }

    companion object {
        fun fromKey(key: String?): EnhancePreset =
            entries.firstOrNull { it.storageKey == key } ?: GLOW
    }
}
