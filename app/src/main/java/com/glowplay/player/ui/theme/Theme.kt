package com.glowplay.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlowScheme = darkColorScheme(
    primary = GlowCyan,
    onPrimary = Night,
    primaryContainer = GlowCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = GlowMagenta,
    onSecondary = Night,
    secondaryContainer = GlowMagentaDeep,
    onSecondaryContainer = TextPrimary,
    tertiary = GlowViolet,
    onTertiary = Night,
    background = Night,
    onBackground = TextPrimary,
    surface = NightElevated,
    onSurface = TextPrimary,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondary,
    outline = NightStroke,
    outlineVariant = NightStrokeSoft,
    error = Danger,
    onError = Color.White,
    scrim = NightDeep,
)

@Composable
fun GlowPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlowScheme,
        typography = GlowTypography,
        content = content,
    )
}
