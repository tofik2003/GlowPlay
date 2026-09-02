package com.glowplay.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.glowplay.player.data.local.ThemeMode

private val GlowDarkScheme = darkColorScheme(
    primary = GlowCyan,
    onPrimary = Night,
    primaryContainer = GlowCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = GlowMagenta,
    onSecondary = Night,
    secondaryContainer = NightCard,
    onSecondaryContainer = TextPrimary,
    tertiary = GlowLime,
    background = Night,
    onBackground = TextPrimary,
    surface = NightElevated,
    onSurface = TextPrimary,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondary,
    outline = NightStroke,
    error = Danger,
    onError = Color.White,
)

private val PremiumLightScheme = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = Color.White,
    primaryContainer = VioletWash,
    onPrimaryContainer = RoyalVioletDeep,
    secondary = Champagne,
    onSecondary = Color.White,
    secondaryContainer = ChampagneWash,
    onSecondaryContainer = Color(0xFF5C4712),
    tertiary = RoyalVioletDeep,
    background = Porcelain,
    onBackground = Ink,
    surface = PureSurface,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = LightStroke,
    error = DangerLight,
    onError = Color.White,
)

@Composable
fun GlowPlayTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) GlowDarkScheme else PremiumLightScheme,
        typography = GlowTypography,
        content = content,
    )
}
