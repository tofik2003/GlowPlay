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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.glowplay.player.R
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.ui.components.AmbientFrame
import com.glowplay.player.ui.components.enhancePresetLabel
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.GlowMagenta
import com.glowplay.player.ui.theme.Night
import com.glowplay.player.ui.theme.TextPrimary
import com.glowplay.player.ui.theme.TextSecondary
import com.glowplay.player.util.TimeFormatter
import kotlin.math.abs

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
    onHoldBoost: (Boolean) -> Unit,
    onPreset: (EnhancePreset) -> Unit,
    onEnhance: ((EnhanceSettings) -> EnhanceSettings) -> Unit,
    onEnhanceOpen: (Boolean) -> Unit,
    onEqOpen: (Boolean) -> Unit,
    onEqPreset: (PlayerViewModel.EqKind) -> Unit,
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
        AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    AmbientFrame(intensity = if (state.enhance.enabled) state.enhance.glow else 0.22f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Night)
                .pointerInput(state.locked, state.preferences.gesturesEnabled, state.preferences.longPressSpeed) {
                    detectTapGestures(
                        onTap = {
                            if (!state.locked) onToggleControls()
                        },
                        onDoubleTap = { offset ->
                            if (state.locked) return@detectTapGestures
                            if (offset.x < size.width / 2f) onSeekBy(-10_000) else onSeekBy(10_000)
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
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val left = change.position.x < size.width / 2f
                            if (left) {
                                val lp = window?.attributes ?: return@detectVerticalDragGestures
                                val current = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                val next = (current - dragAmount / size.height).coerceIn(0.01f, 1f)
                                lp.screenBrightness = next
                                window.attributes = lp
                            } else {
                                if (abs(dragAmount) > 12) {
                                    audioManager?.adjustStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        if (dragAmount < 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                                        0,
                                    )
                                }
                            }
                        },
                    )
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

            if (state.holdBoost) {
                Text(
                    text = "2×",
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                        }
                        Text(
                            text = state.title,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
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

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Outlined.SkipPrevious, contentDescription = stringResource(R.string.previous), tint = Color.White, modifier = Modifier.size(36.dp))
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
                        IconButton(onClick = onNext) {
                            Icon(Icons.Outlined.SkipNext, contentDescription = stringResource(R.string.next), tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

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
                            IconButton(onClick = {
                                val next = when (state.speed) {
                                    1f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2f
                                    2f -> 0.75f
                                    else -> 1f
                                }
                                onSpeed(next)
                            }) {
                                Icon(Icons.Outlined.Speed, contentDescription = stringResource(R.string.playback_speed), tint = Color.White)
                            }
                            Text("${state.speed}×", color = GlowCyan, modifier = Modifier.padding(end = 8.dp))
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
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
            if (state.eqOpen && !state.locked) {
                EqPanel(
                    onEqPreset = onEqPreset,
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

    DisposableEffect(Unit) {
        onDispose { onHoldBoost(false) }
    }
}

@Composable
private fun EnhancePanel(
    state: PlayerUiState,
    onPreset: (EnhancePreset) -> Unit,
    onEnhance: ((EnhanceSettings) -> EnhanceSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xEE0B111A))
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.enhance), color = GlowCyan)
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
                    label = { Text(enhancePresetLabel(preset)) },
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
        EnhanceSlider(stringResource(R.string.ambient_glow), state.enhance.glow * 2f - 1f) {
            onEnhance { s -> s.copy(glow = ((it + 1f) / 2f).coerceIn(0f, 1f), enabled = true) }
        }
    }
}

@Composable
private fun EnhanceSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondary, modifier = Modifier.fillMaxWidth(0.28f), fontSize = 12.sp)
        Slider(
            value = value.coerceIn(-1f, 1f),
            onValueChange = onChange,
            valueRange = -1f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = GlowMagenta, activeTrackColor = GlowCyan),
        )
    }
}

@Composable
private fun EqPanel(
    onEqPreset: (PlayerViewModel.EqKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xEE0B111A))
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.equalizer), color = GlowCyan)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqChip(stringResource(R.string.eq_normal)) { onEqPreset(PlayerViewModel.EqKind.FLAT) }
            EqChip(stringResource(R.string.eq_bass)) { onEqPreset(PlayerViewModel.EqKind.BASS) }
            EqChip(stringResource(R.string.eq_treble)) { onEqPreset(PlayerViewModel.EqKind.TREBLE) }
            EqChip(stringResource(R.string.eq_vocal)) { onEqPreset(PlayerViewModel.EqKind.VOICE) }
            EqChip(stringResource(R.string.eq_movie)) { onEqPreset(PlayerViewModel.EqKind.MOVIE) }
        }
    }
}

@Composable
private fun EqChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlowCyan.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

fun rotateActivity(activity: Activity) {
    activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
