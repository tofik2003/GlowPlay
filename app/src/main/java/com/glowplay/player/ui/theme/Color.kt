package com.glowplay.player.ui.theme

import androidx.compose.ui.graphics.Color

// ── Aurora Night — player / dark palette ────────────────────────────────────
// A calmer cinema-grade neon: still glows, but easier on the eyes than pure
// saturated cyan/magenta for long viewing sessions.
val Night = Color(0xFF05070C)
val NightElevated = Color(0xFF0B0F17)
val NightCard = Color(0xFF121826)
val NightCardHigh = Color(0xFF19202F)
val NightStroke = Color(0x2E4DE8F2)
val GlowCyan = Color(0xFF22E1F2)
val GlowCyanDim = Color(0xFF0E7C8C)
val GlowCyanSoft = Color(0xFF9FF3FA)
val GlowMagenta = Color(0xFFFF4FD8)
val GlowLime = Color(0xFF7CFF6B)
val GlowAmber = Color(0xFFFFC46B)
val TextPrimary = Color(0xFFF4FBFF)
val TextSecondary = Color(0xFFA6B6C6)
val Danger = Color(0xFFFF5C7A)

// ── Aurora Light — premium default palette ──────────────────────────────────
// Warm paper background with a refined indigo/teal/gold accent trio instead
// of a single hue, tuned to Material 3 tonal roles (container / on-container).
val Paper = Color(0xFFFBFAF7)          // app background
val PureSurface = Color(0xFFFFFFFF)    // resting cards
val SurfaceLow = Color(0xFFF6F3EC)     // recessed surfaces
val SurfaceContainer = Color(0xFFF1ECE1)
val SurfaceContainerHigh = Color(0xFFE9E2D3)
val SurfaceContainerHighest = Color(0xFFE1D8C4)
val Ink = Color(0xFF1B1B1F)            // primary text
val Slate = Color(0xFF63605A)          // secondary text
val Hairline = Color(0x1F1B1B1F)

val Indigo = Color(0xFF5B4CDB)          // primary accent (kept from v2 identity)
val IndigoDeep = Color(0xFF352B8C)
val IndigoWash = Color(0xFFE7E3FB)

val Teal = Color(0xFF0C8C86)            // secondary "glow" accent
val TealDeep = Color(0xFF04413E)
val TealWash = Color(0xFFD8F0EC)

val Champagne = Color(0xFFAD7E1E)       // tertiary gold accent
val ChampagneDeep = Color(0xFF5C4712)
val ChampagneWash = Color(0xFFF4E9CE)

val DangerLight = Color(0xFFBA1B33)
val DangerLightWash = Color(0xFFFCE7EA)

// Legacy aliases kept for call sites and docs that still reference the v2
// naming (RoyalViolet family maps 1:1 onto the refreshed Indigo tokens).
val RoyalViolet = Indigo
val RoyalVioletDeep = IndigoDeep
val VioletWash = IndigoWash
val Porcelain = Paper
val Mist = SurfaceLow
val LightStroke = Hairline
