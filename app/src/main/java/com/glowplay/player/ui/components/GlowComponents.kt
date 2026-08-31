package com.glowplay.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowplay.player.R
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.FilmLook
import com.glowplay.player.ui.theme.Glass
import com.glowplay.player.ui.theme.GlowAmber
import com.glowplay.player.ui.theme.GlowBlue
import com.glowplay.player.ui.theme.GlowCoral
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.GlowCyanBright
import com.glowplay.player.ui.theme.GlowGradients
import com.glowplay.player.ui.theme.GlowLime
import com.glowplay.player.ui.theme.GlowMagenta
import com.glowplay.player.ui.theme.GlowViolet
import com.glowplay.player.ui.theme.NightCard
import com.glowplay.player.ui.theme.TabularTextStyle
import com.glowplay.player.ui.theme.TextPrimary
import com.glowplay.player.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glow: Color = GlowCyan,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(NightCard)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(glow.copy(alpha = 0.55f), GlowMagenta.copy(alpha = 0.25f))),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(1.dp),
        content = content,
    )
}

@Composable
fun GlowTitle(text: String, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "title-pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "title-alpha",
    )
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            shadow = Shadow(
                color = GlowCyan.copy(alpha = alpha),
                offset = Offset.Zero,
                blurRadius = 22f,
            ),
        ),
    )
}

@Composable
fun AmbientFrame(
    intensity: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val glow = intensity.coerceIn(0f, 1f)
    Box(
        modifier = modifier.drawBehind {
            if (glow <= 0.01f) return@drawBehind
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlowCyan.copy(alpha = 0.18f * glow),
                        GlowMagenta.copy(alpha = 0.10f * glow),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.72f,
                ),
            )
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun enhancePresetLabel(preset: EnhancePreset): String = when (preset) {
    EnhancePreset.OFF -> stringResource(R.string.preset_off)
    EnhancePreset.GLOW -> stringResource(R.string.preset_glow)
    EnhancePreset.CINEMA -> stringResource(R.string.preset_cinema)
    EnhancePreset.VIVID -> stringResource(R.string.preset_vivid)
    EnhancePreset.NIGHT -> stringResource(R.string.preset_night)
    EnhancePreset.CRYSTAL -> stringResource(R.string.preset_crystal)
    EnhancePreset.WARM -> stringResource(R.string.preset_warm)
    EnhancePreset.COOL -> stringResource(R.string.preset_cool)
    EnhancePreset.CUSTOM -> stringResource(R.string.preset_custom)
}

@Composable
fun filmLookLabel(look: FilmLook): String = when (look) {
    FilmLook.NONE -> stringResource(R.string.look_none)
    FilmLook.NOIR -> stringResource(R.string.look_noir)
    FilmLook.TEAL -> stringResource(R.string.look_teal)
    FilmLook.FADE -> stringResource(R.string.look_fade)
    FilmLook.VINTAGE -> stringResource(R.string.look_vintage)
}

/** Swatch gradient for each color-grade preset. */
fun presetSwatchColors(preset: EnhancePreset): List<Color> = when (preset) {
    EnhancePreset.OFF -> listOf(Color(0xFF2A3442), Color(0xFF141B26))
    EnhancePreset.GLOW -> GlowGradients.BrandTri
    EnhancePreset.CINEMA -> listOf(Color(0xFF3A2C54), GlowMagenta)
    EnhancePreset.VIVID -> listOf(GlowMagenta, GlowAmber)
    EnhancePreset.NIGHT -> listOf(Color(0xFF1C2740), GlowBlue)
    EnhancePreset.CRYSTAL -> GlowGradients.Cool
    EnhancePreset.WARM -> GlowGradients.Warm
    EnhancePreset.COOL -> listOf(GlowBlue, GlowViolet)
    EnhancePreset.CUSTOM -> listOf(GlowCyan, GlowMagenta, GlowLime)
}

/** Swatch gradient for each film look. */
fun filmLookSwatchColors(look: FilmLook): List<Color> = when (look) {
    FilmLook.NONE -> listOf(Color(0xFF2A3442), Color(0xFF141B26))
    FilmLook.NOIR -> GlowGradients.Mono
    FilmLook.TEAL -> listOf(GlowCyan, GlowBlue)
    FilmLook.FADE -> listOf(Color(0xFF9DB8CC), Color(0xFFE8C8B0))
    FilmLook.VINTAGE -> listOf(GlowAmber, GlowCoral)
}

@Composable
fun ResumeBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(GlowCyan, GlowMagenta)),
                ),
        )
    }
}

/**
 * The translucent, top-rounded surface every player sheet (enhance, equalizer,
 * tracks) sits on, so they all share the same glass look.
 */
@Composable
fun BottomSheetSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Glass)
            .border(
                width = 1.dp,
                brush = GlowGradients.horizontal(GlowGradients.Brand.map { it.copy(alpha = 0.35f) }),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            )
            .padding(16.dp),
        content = content,
    )
}

/** Small uppercase neon section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.US),
        color = GlowCyan,
        style = MaterialStyleLabel,
        modifier = modifier,
    )
}

/**
 * A vertically stacked gradient swatch card used for color-grade presets and
 * film looks. The tile previews the look; the cyan frame marks the selection.
 */
@Composable
fun SwatchCard(
    label: String,
    selected: Boolean,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tileHeight: androidx.compose.ui.unit.Dp = 34.dp,
    tileWidth: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(tileWidth, tileHeight)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.horizontalGradient(colors))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) GlowCyan else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(11.dp),
                ),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GlowCyan),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/**
 * Premium slider row: label, slider, live value badge, and a reset dot that
 * appears whenever the value has drifted from its default.
 */
@Composable
fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    isDefault: Boolean,
    onReset: () -> Unit,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GlowMagenta,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(label, color = TextSecondary, modifier = Modifier.width(96.dp), fontSize = 12.sp)
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = GlowCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
            ),
        )
        Text(
            text = valueText,
            style = TabularTextStyle.copy(fontSize = 11.sp, color = TextSecondary),
            modifier = Modifier.width(46.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .size(20.dp)
                .clip(CircleShape)
                .then(if (isDefault) Modifier else Modifier.clickable(onClick = onReset)),
            contentAlignment = Alignment.Center,
        ) {
            if (!isDefault) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(GlowCyan),
                )
            }
        }
    }
}

/** Small pill button with a neon gradient border (used for reset actions). */
@Composable
fun NeonPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(
                width = 1.dp,
                brush = GlowGradients.horizontal(GlowGradients.Brand),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Signed value used by grade sliders, e.g. "+0.34". */
fun formatSigned(value: Float): String = String.format(Locale.US, "%+.2f", value)

/** Percentage used by film sliders and HUD readouts, e.g. "34%". */
fun formatPercent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"

private val MaterialStyleLabel = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.1.sp,
)
