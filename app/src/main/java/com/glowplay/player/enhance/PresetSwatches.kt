package com.glowplay.player.enhance

import androidx.compose.ui.graphics.Color

/**
 * Pure UI-facing accent colors for each [EnhancePreset], used to render a
 * small color swatch on preset chips so users can recognize a look at a
 * glance instead of reading text only.
 */
object PresetSwatches {
    fun colorFor(preset: EnhancePreset): Color = when (preset) {
        EnhancePreset.OFF -> Color(0xFF9AA5B1)
        EnhancePreset.GLOW -> Color(0xFF22E1F2)
        EnhancePreset.CINEMA -> Color(0xFFB98B4E)
        EnhancePreset.VIVID -> Color(0xFFFF4FD8)
        EnhancePreset.NIGHT -> Color(0xFF6C7BFF)
        EnhancePreset.CRYSTAL -> Color(0xFFB9F1FF)
        EnhancePreset.WARM -> Color(0xFFFFA24B)
        EnhancePreset.COOL -> Color(0xFF4BC7FF)
        EnhancePreset.SUNSET -> Color(0xFFFF7A59)
        EnhancePreset.MONO -> Color(0xFFC8C8C8)
        EnhancePreset.CUSTOM -> Color(0xFF7CFF6B)
    }
}
