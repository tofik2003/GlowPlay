package com.glowplay.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glowplay.player.data.local.ThemeMode

private val GlowDarkScheme = darkColorScheme(
    primary = GlowCyan,
    onPrimary = Night,
    primaryContainer = GlowCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = GlowMagenta,
    onSecondary = Night,
    secondaryContainer = NightCardHigh,
    onSecondaryContainer = TextPrimary,
    tertiary = GlowLime,
    onTertiary = Night,
    background = Night,
    onBackground = TextPrimary,
    surface = NightElevated,
    onSurface = TextPrimary,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Night,
    surfaceContainerLow = NightElevated,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCardHigh,
    surfaceContainerHighest = Color(0xFF212A3C),
    outline = NightStroke,
    outlineVariant = Color(0x1AFFFFFF),
    error = Danger,
    onError = Color.White,
)

private val AuroraLightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoWash,
    onPrimaryContainer = IndigoDeep,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealWash,
    onSecondaryContainer = TealDeep,
    tertiary = Champagne,
    onTertiary = Color.White,
    tertiaryContainer = ChampagneWash,
    onTertiaryContainer = ChampagneDeep,
    background = Paper,
    onBackground = Ink,
    surface = PureSurface,
    onSurface = Ink,
    surfaceVariant = SurfaceLow,
    onSurfaceVariant = Slate,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Hairline,
    outlineVariant = Color(0x141B1B1F),
    error = DangerLight,
    onError = Color.White,
    errorContainer = DangerLightWash,
    onErrorContainer = DangerLight,
)

/** Rounded-modern shape scale shared across the whole app. */
val GlowShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
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
        colorScheme = if (dark) GlowDarkScheme else AuroraLightScheme,
        typography = GlowTypography,
        shapes = GlowShapes,
        content = content,
    )
}
