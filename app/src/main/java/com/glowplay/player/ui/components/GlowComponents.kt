package com.glowplay.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.GlowMagenta

/**
 * Premium surface card: soft shadow, hairline outline, rounded corners.
 * Adapts to the active color scheme (ivory light / night dark).
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glow: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = glow.copy(alpha = 0.35f),
                ambientColor = glow.copy(alpha = 0.20f),
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = shape,
            ),
        content = content,
    )
}

@Composable
fun GlowTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
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
fun ResumeBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.30f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                ),
        )
    }
}
