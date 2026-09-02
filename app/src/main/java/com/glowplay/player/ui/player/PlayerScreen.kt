package com.glowplay.player.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.glowplay.player.R
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.ui.components.AmbientFrame
import com.glowplay.player.ui.theme.GlowAmber
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.Night
import com.glowplay.player.ui.theme.TextPrimary
import com.glowplay.player.ui.theme.TextSecondary
import com.glowplay.player.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class DragAxis { NONE, HORIZONTAL, VERTICAL }

private enum class HudKind { VOLUME, BRIGHTNESS, SEEK }

private data class GestureHud(
    val kind: HudKind,
    val fraction: Float,
    val text: String,
    val subText: String = "",
)

private data class TapFlash(val forward: Boolean, val seconds: Int, val id: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    playerViewFactory: () -> PlayerView,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleControls: () -> Unit,
    onShowControls: () -> Unit,
    onCycleAspect: () -> Unit,
    onSpeed: (Float) -> Unit,
    onSpeedOpen: (Boolean) -> Unit,
    onHoldBoost: (Boolean) -> Unit,
    onPreset: (EnhancePreset) -> Unit,
    onEnhance: ((EnhanceSettings) -> EnhanceSettings) -> Unit,
    onEnhanceReset: () -> Unit,
    onEnhanceOpen: (Boolean) -> Unit,
    onEqOpen: (Boolean) -> Unit,
    onEqPreset: (PlayerViewModel.EqKind) -> Unit,
    onSelectAudio: (Int) -> Unit,
    onSelectText: (Int) -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val activity = context as? Activity
    val window = activity?.window
    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val resizeMode = when (state.aspect) {
        AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    var hud by remember { mutableStateOf<GestureHud?>(null) }
    var tapFlash by remember { mutableStateOf<TapFlash?>(null) }
    var aspectFlash by remember { mutableStateOf<String?>(null) }
    var audioDialog by remember { mutableStateOf(false) }
    var textDialog by remember { mutableStateOf(false) }

    val positionState = rememberUpdatedState(state.positionMs)
    val durationState = rememberUpdatedState(state.durationMs)
    val seekStepMs = state.preferences.seekStepSeconds * 1000L

    // Transient aspect-ratio label (MX-style toast)
    val aspectLabel = when (state.aspect) {
        AspectMode.FIT -> stringResource(R.string.aspect_fit)
        AspectMode.STRETCH -> stringResource(R.string.aspect_stretch)
        AspectMode.CROP -> stringResource(R.string.aspect_crop)
    }
    var aspectSeen by remember { mutableStateOf(false) }
    LaunchedEffect(state.aspect) {
        if (!aspectSeen) {
            aspectSeen = true
        } else {
            aspectFlash = aspectLabel
            kotlinx.coroutines.delay(1_000)
            aspectFlash = null
        }
    }
    LaunchedEffect(tapFlash) {
        if (tapFlash != null) {
            kotlinx.coroutines.delay(700)
            tapFlash = null
        }
    }

    AmbientFrame(intensity = if (state.enhance.enabled) state.enhance.glow else 0.22f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Night)
                .pointerInput(state.locked, seekStepMs, state.preferences.longPressSpeed) {
                    detectTapGestures(
                        onTap = {
                            if (!state.locked) onToggleControls()
                        },
                        onDoubleTap = { offset ->
                            if (state.locked) return@detectTapGestures
                            val forward = offset.x >= size.width / 2f
                            onSeekBy(if (forward) seekStepMs else -seekStepMs)
                            tapFlash = TapFlash(forward, state.preferences.seekStepSeconds, System.nanoTime())
                            onShowControls()
                        },
                        onLongPress = {
                            if (!state.locked) onHoldBoost(true)
                        },
                        onPress = {
                            try {
                                awaitRelease()
                            } finally {
                                onHoldBoost(false)
                            }
                        },
                    )
                }
                .pointerInput(state.locked, state.preferences.gesturesEnabled) {
                    if (state.locked || !state.preferences.gesturesEnabled) return@pointerInput
                    var axis = DragAxis.NONE
                    var accumulated = Offset.Zero
                    var leftSide = false
                    var volumeAccum = 0f
                    var brightnessAccum = 0.5f
                    var seekBase = 0L
                    var seekTarget = 0L
                    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                    detectDragGestures(
                        onDragStart = { offset ->
                            axis = DragAxis.NONE
                            accumulated = Offset.Zero
                            leftSide = offset.x < size.width / 2f
                            volumeAccum = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat()
                            val lp = window?.attributes
                            brightnessAccum = if (lp == null || lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                            seekBase = positionState.value
                            seekTarget = seekBase
                        },
                        onDragEnd = {
                            if (axis == DragAxis.HORIZONTAL && durationState.value > 0) {
                                onSeek(seekTarget)
                            }
                            hud = null
                        },
                        onDragCancel = { hud = null },
                    ) { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount
                        if (axis == DragAxis.NONE && accumulated.getDistance() > 26f) {
                            axis = if (abs(accumulated.x) > abs(accumulated.y)) {
                                DragAxis.HORIZONTAL
                            } else {
                                DragAxis.VERTICAL
                            }
                        }
                        when (axis) {
                            DragAxis.HORIZONTAL -> {
                                val duration = durationState.value
                                if (duration > 0) {
                                    val deltaMs = (accumulated.x / size.width * 180_000f).toLong()
                                    seekTarget = (seekBase + deltaMs).coerceIn(0L, duration)
                                    val signed = seekTarget - seekBase
                                    val sign = if (signed >= 0) "+" else "-"
                                    hud = GestureHud(
                                        kind = HudKind.SEEK,
                                        fraction = TimeFormatter.progress(seekTarget, duration),
                                        text = "$sign${TimeFormatter.formatMs(abs(signed))}",
                                        subText = "${TimeFormatter.formatMs(seekTarget)} / ${TimeFormatter.formatMs(duration)}",
                                    )
                                }
                            }
                            DragAxis.VERTICAL -> {
                                if (leftSide) {
                                    if (window != null) {
                                        val lp = window.attributes
                                        brightnessAccum = (brightnessAccum - dragAmount.y / (size.height * 0.75f))
                                            .coerceIn(0.01f, 1f)
                                        lp.screenBrightness = brightnessAccum
                                        window.attributes = lp
                                        hud = GestureHud(
                                            kind = HudKind.BRIGHTNESS,
                                            fraction = brightnessAccum,
                                            text = "${(brightnessAccum * 100).roundToInt()}%",
                                        )
                                    }
                                } else if (audioManager != null) {
                                    volumeAccum = (volumeAccum - dragAmount.y / (size.height * 0.75f) * maxVolume)
                                        .coerceIn(0f, maxVolume.toFloat())
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        volumeAccum.roundToInt().coerceIn(0, maxVolume),
                                        0,
                                    )
                                    hud = GestureHud(
                                        kind = HudKind.VOLUME,
                                        fraction = volumeAccum / maxVolume,
                                        text = "${(volumeAccum / maxVolume * 100).roundToInt()}%",
                                    )
                                }
                            }
                            DragAxis.NONE -> Unit
                        }
                    }
                },
        ) {
            AndroidView(
                factory = { playerViewFactory() },
                update = { view ->
                    view.resizeMode = resizeMode
                    view.useController = false
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (state.buffering) {
                CircularProgressIndicator(
                    color = GlowCyan,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // ── Gesture HUD (volume / brightness / seek preview) ────────────
            hud?.let { current ->
                GestureHudOverlay(current, modifier = Modifier.align(Alignment.Center))
            }

            // ── Double-tap seek flash ───────────────────────────────────────
            tapFlash?.let { flash ->
                Text(
                    text = if (flash.forward) "+${flash.seconds}s ⏩" else "⏪ -${flash.seconds}s",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(if (flash.forward) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(horizontal = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            // ── Aspect ratio label flash ────────────────────────────────────
            aspectFlash?.let { label ->
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.holdBoost) {
                Text(
                    text = formatSpeed(state.preferences.holdSpeedValue),
                    color = GlowCyan,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.locked) {
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = stringResource(R.string.unlock), tint = GlowCyan)
                }
            }

            AnimatedVisibility(
                visible = state.controlsVisible && !state.locked,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.34f)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.42f)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                                ),
                            ),
                    )

                    // ── Top bar: back · title · clock · track/enhance actions ─
                    TopBar(
                        state = state,
                        onBack = onBack,
                        onAudio = { audioDialog = true },
                        onText = { textDialog = true },
                        onEnhanceOpen = onEnhanceOpen,
                        onEqOpen = onEqOpen,
                        onPip = onPip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp),
                    )

                    // ── Center transport ─────────────────────────────────────
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Outlined.SkipPrevious, contentDescription = stringResource(R.string.previous), tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        IconButton(onClick = { onSeekBy(-seekStepMs) }) {
                            Icon(Icons.Outlined.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(GlowCyan.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.4f))),
                                ),
                        ) {
                            Icon(
                                if (state.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = stringResource(if (state.playing) R.string.pause else R.string.play),
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        IconButton(onClick = { onSeekBy(seekStepMs) }) {
                            Icon(Icons.Outlined.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Outlined.SkipNext, contentDescription = stringResource(R.string.next), tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }

                    // ── Bottom bar: seek + secondary controls ────────────────
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(TimeFormatter.formatMs(state.positionMs), color = TextSecondary, fontSize = 12.sp)
                            Slider(
                                value = TimeFormatter.progress(state.positionMs, state.durationMs),
                                onValueChange = { fraction ->
                                    if (state.durationMs > 0) onSeek((fraction * state.durationMs).toLong())
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = GlowCyan,
                                    activeTrackColor = GlowCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                ),
                            )
                            Text(TimeFormatter.formatMs(state.durationMs), color = TextSecondary, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onToggleLock) {
                                Icon(Icons.Outlined.LockOpen, contentDescription = stringResource(R.string.lock), tint = Color.White)
                            }
                            IconButton(onClick = onCycleAspect) {
                                Icon(Icons.Outlined.AspectRatio, contentDescription = stringResource(R.string.aspect_ratio), tint = Color.White)
                            }
                            TextButton(onClick = { onSpeedOpen(!state.speedOpen) }) {
                                Text(
                                    text = formatSpeed(state.speed),
                                    color = if (state.speed == 1f) Color.White else GlowAmber,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            IconButton(onClick = onRotate) {
                                Icon(Icons.Outlined.Rotate90DegreesCcw, contentDescription = stringResource(R.string.rotate), tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (state.enhanceOpen && !state.locked) {
                EnhancePanel(
                    state = state,
                    onPreset = onPreset,
                    onEnhance = onEnhance,
                    onReset = onEnhanceReset,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
            if (state.eqOpen && !state.locked) {
                EqPanel(
                    selected = state.eqKind,
                    onEqPreset = onEqPreset,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
            if (state.speedOpen && !state.locked) {
                SpeedPanel(
                    speed = state.speed,
                    onSpeed = onSpeed,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFFF8CA0),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(10.dp),
                )
            }
        }
    }

    if (audioDialog) {
        TrackDialog(
            title = stringResource(R.string.audio_track),
            options = state.audioTracks,
            selected = state.selectedAudio,
            allowOff = false,
            onSelect = {
                onSelectAudio(it)
                audioDialog = false
            },
            onDismiss = { audioDialog = false },
        )
    }
    if (textDialog) {
        TrackDialog(
            title = stringResource(R.string.subtitles),
            options = state.textTracks,
            selected = state.selectedText,
            allowOff = true,
            onSelect = {
                onSelectText(it)
                textDialog = false
            },
            onDismiss = { textDialog = false },
        )
    }

    DisposableEffect(Unit) {
        onDispose { onHoldBoost(false) }
    }
}

@Composable
private fun TopBar(
    state: PlayerUiState,
    onBack: () -> Unit,
    onAudio: () -> Unit,
    onText: () -> Unit,
    onEnhanceOpen: (Boolean) -> Unit,
    onEqOpen: (Boolean) -> Unit,
    onPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var clock by remember { mutableStateOf(currentClock()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = currentClock()
            kotlinx.coroutines.delay(20_000)
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = state.title,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = clock, color = TextSecondary, fontSize = 11.sp)
        }
        if (state.audioTracks.size > 1) {
            IconButton(onClick = onAudio) {
                Icon(Icons.Outlined.Audiotrack, contentDescription = stringResource(R.string.audio_track), tint = Color.White)
            }
        }
        if (state.textTracks.isNotEmpty()) {
            IconButton(onClick = onText) {
                Icon(Icons.Outlined.Subtitles, contentDescription = stringResource(R.string.subtitles), tint = Color.White)
            }
        }
        IconButton(onClick = { onEnhanceOpen(!state.enhanceOpen) }) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = stringResource(R.string.enhance), tint = GlowCyan)
        }
        IconButton(onClick = { onEqOpen(!state.eqOpen) }) {
            Icon(Icons.Outlined.Equalizer, contentDescription = stringResource(R.string.equalizer), tint = Color.White)
        }
        IconButton(onClick = onPip) {
            Icon(Icons.Outlined.PictureInPictureAlt, contentDescription = stringResource(R.string.pip), tint = Color.White)
        }
    }
}

@Composable
private fun GestureHudOverlay(hud: GestureHud, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon: ImageVector? = when (hud.kind) {
            HudKind.VOLUME -> Icons.AutoMirrored.Outlined.VolumeUp
            HudKind.BRIGHTNESS -> Icons.Outlined.BrightnessMedium
            HudKind.SEEK -> null
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
        }
        Text(hud.text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (hud.subText.isNotEmpty()) {
            Text(hud.subText, color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(hud.fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(GlowCyan),
            )
        }
    }
}

@Composable
private fun TrackDialog(
    title: String,
    options: List<String>,
    selected: Int,
    allowOff: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (allowOff) {
                    TrackRow(
                        label = stringResource(R.string.track_off),
                        selected = selected < 0,
                        onClick = { onSelect(-1) },
                    )
                }
                options.forEachIndexed { index, label ->
                    TrackRow(
                        label = label,
                        selected = selected == index,
                        onClick = { onSelect(index) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PanelSurface(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xF20B111A))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun EnhancePanel(
    state: PlayerUiState,
    onPreset: (EnhancePreset) -> Unit,
    onEnhance: ((EnhanceSettings) -> EnhanceSettings) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelSurface(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.enhance), color = GlowCyan, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.reset), color = TextSecondary)
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EnhancePreset.entries.filter { it != EnhancePreset.CUSTOM }.forEach { preset ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { onPreset(preset) },
                    label = { Text(presetLabel(preset)) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = TextSecondary,
                        selectedLabelColor = Night,
                        selectedContainerColor = GlowCyan,
                    ),
                )
            }
        }
        EnhanceSlider(stringResource(R.string.brightness), state.enhance.brightness) {
            onEnhance { s -> s.copy(brightness = it) }
        }
        EnhanceSlider(stringResource(R.string.contrast), state.enhance.contrast) {
            onEnhance { s -> s.copy(contrast = it) }
        }
        EnhanceSlider(stringResource(R.string.saturation), state.enhance.saturation) {
            onEnhance { s -> s.copy(saturation = it) }
        }
        EnhanceSlider(stringResource(R.string.warmth), state.enhance.warmth) {
            onEnhance { s -> s.copy(warmth = it) }
        }
        EnhanceSlider(stringResource(R.string.hue), state.enhance.hue) {
            onEnhance { s -> s.copy(hue = it) }
        }
        EnhanceSlider(stringResource(R.string.tint), state.enhance.tint) {
            onEnhance { s -> s.copy(tint = it) }
        }
        EnhanceSlider(stringResource(R.string.ambient_glow), state.enhance.glow * 2f - 1f) {
            onEnhance { s -> s.copy(glow = ((it + 1f) / 2f).coerceIn(0f, 1f), enabled = true) }
        }
    }
}

@Composable
private fun EnhanceSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondary, modifier = Modifier.fillMaxWidth(0.26f), fontSize = 12.sp)
        Slider(
            value = value.coerceIn(-1f, 1f),
            onValueChange = onChange,
            valueRange = -1f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = GlowCyan, activeTrackColor = GlowCyan),
        )
        Text(
            text = formatEnhanceValue(value),
            color = if (abs(value) > 0.005f) GlowCyan else TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(38.dp),
        )
    }
}

@Composable
private fun EqPanel(
    selected: PlayerViewModel.EqKind,
    onEqPreset: (PlayerViewModel.EqKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelSurface(modifier = modifier) {
        Text(stringResource(R.string.equalizer), color = GlowCyan, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqChip(stringResource(R.string.eq_normal), selected == PlayerViewModel.EqKind.FLAT) { onEqPreset(PlayerViewModel.EqKind.FLAT) }
            EqChip(stringResource(R.string.eq_bass), selected == PlayerViewModel.EqKind.BASS) { onEqPreset(PlayerViewModel.EqKind.BASS) }
            EqChip(stringResource(R.string.eq_treble), selected == PlayerViewModel.EqKind.TREBLE) { onEqPreset(PlayerViewModel.EqKind.TREBLE) }
            EqChip(stringResource(R.string.eq_vocal), selected == PlayerViewModel.EqKind.VOICE) { onEqPreset(PlayerViewModel.EqKind.VOICE) }
            EqChip(stringResource(R.string.eq_movie), selected == PlayerViewModel.EqKind.MOVIE) { onEqPreset(PlayerViewModel.EqKind.MOVIE) }
        }
    }
}

@Composable
private fun EqChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Night else TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GlowCyan else GlowCyan.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun SpeedPanel(
    speed: Float,
    onSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelSurface(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.playback_speed), color = GlowCyan, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(formatSpeed(speed), color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = speed,
            onValueChange = { onSpeed((it * 20).roundToInt() / 20f) },
            valueRange = 0.25f..4f,
            colors = SliderDefaults.colors(thumbColor = GlowCyan, activeTrackColor = GlowCyan),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f).forEach { option ->
                EqChip(formatSpeed(option), speed == option) { onSpeed(option) }
            }
        }
    }
}

@Composable
private fun presetLabel(preset: EnhancePreset): String = when (preset) {
    EnhancePreset.OFF -> stringResource(R.string.preset_off)
    EnhancePreset.GLOW -> stringResource(R.string.preset_glow)
    EnhancePreset.CINEMA -> stringResource(R.string.preset_cinema)
    EnhancePreset.VIVID -> stringResource(R.string.preset_vivid)
    EnhancePreset.NIGHT -> stringResource(R.string.preset_night)
    EnhancePreset.CRYSTAL -> stringResource(R.string.preset_crystal)
    EnhancePreset.WARM -> stringResource(R.string.preset_warm)
    EnhancePreset.COOL -> stringResource(R.string.preset_cool)
    EnhancePreset.SUNSET -> stringResource(R.string.preset_sunset)
    EnhancePreset.MONO -> stringResource(R.string.preset_mono)
    EnhancePreset.CUSTOM -> stringResource(R.string.preset_custom)
}

private fun currentClock(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun formatSpeed(speed: Float): String {
    val rounded = (speed * 100).roundToInt() / 100f
    return if (rounded == rounded.toInt().toFloat()) {
        "${rounded.toInt()}.0×"
    } else {
        "$rounded×"
    }
}

private fun formatEnhanceValue(value: Float): String {
    val pct = (value * 100).roundToInt()
    return if (pct > 0) "+$pct" else "$pct"
}

fun rotateActivity(activity: Activity) {
    activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
