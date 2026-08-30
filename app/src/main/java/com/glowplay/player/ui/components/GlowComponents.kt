package com.glowplay.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.GlowMagenta
import com.glowplay.player.ui.theme.NightCard
import com.glowplay.player.ui.theme.TextPrimary

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
