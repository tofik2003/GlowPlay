package com.glowplay.player.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Reusable neon gradient brushes so every surface uses the same brand light. */
object GlowGradients {

    /** Cyan -> violet -> magenta brand sweep. */
    val BrandTri = listOf(GlowCyan, GlowViolet, GlowMagenta)

    /** Cyan -> magenta, the compact brand gradient. */
    val Brand = listOf(GlowCyan, GlowMagenta)

    /** Cyan -> blue, used for the cool/teal family. */
    val Cool = listOf(GlowCyan, GlowBlue)

    /** Magenta -> amber, used for the warm family. */
    val Warm = listOf(GlowMagenta, GlowAmber)

    /** Monochrome slate for the noir family. */
    val Mono = listOf(GlowCyanBright, TextSecondary)

    fun horizontal(colors: List<Color>): Brush = Brush.horizontalGradient(colors)

    fun vertical(colors: List<Color>): Brush = Brush.verticalGradient(colors)

    fun radial(colors: List<Color>, center: Offset, radius: Float): Brush =
        Brush.radialGradient(colors, center = center, radius = radius)
}
