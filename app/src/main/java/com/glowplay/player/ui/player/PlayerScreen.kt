package com.glowplay.player.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.media3.ui.SubtitleView
import com.glowplay.player.R
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.FilmLook
import com.glowplay.player.playback.EqKind
import com.glowplay.player.ui.components.AmbientFrame
import com.glowplay.player.ui.components.BottomSheetSurface
import com.glowplay.player.ui.components.NeonPillButton
import com.glowplay.player.ui.components.SectionLabel
import com.glowplay.player.ui.components.SliderRow
import com.glowplay.player.ui.components.SwatchCard
import com.glowplay.player.ui.components.enhancePresetLabel
import com.glowplay.player.ui.components.filmLookLabel
import com.glowplay.player.ui.components.filmLookSwatchColors
import com.glowplay.player.ui.components.formatPercent
import com.glowplay.player.ui.components.formatSigned
import com.glowplay.player.ui.components.presetSwatchColors
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.GlowMagenta
import com.glowplay.player.ui.theme.Night
import com.glowplay.player.ui.theme.TabularTextStyle
import com.glowplay.player.ui.theme.TextPrimary
import com.glowplay.player.ui.theme.TextSecondary
import com.glowplay.player.util.TimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

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
    onFilmLook: (FilmLook) -> Unit,
    onResetEnhance: () -> Unit,
    onGestureHud: (GestureHud) -> Unit,
    onEqOpen: (Boolean) -> Unit,
    onEqPreset: (EqKind) -> Unit,
    onLoudness: (Int) -> Unit,
    onBass: (Int) -> Unit,
    onSurround: (Int) -> Unit,
    onTracksOpen: (Boolean) -> Unit,
    onSelectAudio: (Int) -> Unit,
    onSelectText: (Int) -> Unit,
    onSubtitleSize: (Float) -> Unit,
    onSubtitlePosition: (Float) -> Unit,
    onSharpen: (Float) -> Unit,
    onVignette: (Float) -> Unit,
    onGrain: (Float) -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val activity = context as? Activity
    val window = activity?.window
    var speedMenu by remember { mutableStateOf(false) }
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
                            val back = offset.x < size.width / 2f
                            onSeekBy(if (back) -10_000 else 10_000)
                            val frac = if (state.durationMs > 0L) {
                                (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                            } else 0f
                            onGestureHud(GestureHud(HudKind.SEEK, frac, if (back) "-10s" else "+10s"))
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
                                onGestureHud(GestureHud(HudKind.BRIGHTNESS, next, formatPercent(next)))
                            } else {
                                if (abs(dragAmount) > 12) {
                                    audioManager?.adjustStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        if (dragAmount < 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                                        0,
                                    )
                                    val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                                    val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                    val frac = if (max > 0) cur.toFloat() / max else 0f
                                    onGestureHud(GestureHud(HudKind.VOLUME, frac, formatPercent(frac)))
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
                    view.getSubtitleView()?.apply {
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * state.subtitleSize)
                        setBottomPaddingFraction(state.subtitlePosition)
                    }
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
                            Box {
                                IconButton(onClick = { speedMenu = true }) {
                                    Icon(Icons.Outlined.Speed, contentDescription = stringResource(R.string.playback_speed), tint = Color.White)
                                }
                                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                                    speedOptions.forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text(formatSpeed(speed)) },
                                            onClick = {
                                                speedMenu = false
                                                onSpeed(speed)
                                            },
                                            leadingIcon = {
                                                if (speed == state.speed) {
                                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = GlowCyan)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            Text(formatSpeed(state.speed), color = GlowCyan, modifier = Modifier.padding(end = 8.dp))
                            IconButton(onClick = onRotate) {
                                Icon(Icons.Outlined.Rotate90DegreesCcw, contentDescription = stringResource(R.string.rotate), tint = Color.White)
                            }
                            IconButton(onClick = { onTracksOpen(!state.tracksOpen) }) {
                                Icon(Icons.Outlined.Subtitles, contentDescription = stringResource(R.string.subtitles), tint = Color.White)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.enhanceOpen && !state.locked,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                EnhancePanel(
                    state = state,
                    onPreset = onPreset,
                    onEnhance = onEnhance,
                    onFilmLook = onFilmLook,
                    onResetEnhance = onResetEnhance,
                    onSharpen = onSharpen,
                    onVignette = onVignette,
                    onGrain = onGrain,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
            AnimatedVisibility(
                visible = state.eqOpen && !state.locked,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                EqPanel(
                    loudness = state.loudness,
                    bass = state.bass,
                    surround = state.surround,
                    onEqPreset = onEqPreset,
                    onLoudness = onLoudness,
                    onBass = onBass,
                    onSurround = onSurround,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
            AnimatedVisibility(
                visible = state.tracksOpen && !state.locked,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                TracksPanel(
                    audioTracks = state.audioTracks,
                    textTracks = state.textTracks,
                    selectedAudio = state.selectedAudio,
                    selectedText = state.selectedText,
                    subtitleSize = state.subtitleSize,
                    subtitlePosition = state.subtitlePosition,
                    onSelectAudio = onSelectAudio,
                    onSelectText = onSelectText,
                    onSubtitleSize = onSubtitleSize,
                    onSubtitlePosition = onSubtitlePosition,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
            state.gestureHud?.let { hud ->
                GestureHudOverlay(hud)
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
    onFilmLook: (FilmLook) -> Unit,
    onResetEnhance: () -> Unit,
    onSharpen: (Float) -> Unit,
    onVignette: (Float) -> Unit,
    onGrain: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheetSurface(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SectionLabel(stringResource(R.string.enhance), modifier = Modifier.weight(1f))
            NeonPillButton(stringResource(R.string.reset), onClick = onResetEnhance)
        }

        SectionLabel(stringResource(R.string.section_look), modifier = Modifier.padding(top = 12.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilmLook.entries.forEach { look ->
                SwatchCard(
                    label = filmLookLabel(look),
                    selected = state.filmLook == look,
                    colors = filmLookSwatchColors(look),
                    onClick = { onFilmLook(look) },
                )
            }
        }

        SectionLabel(stringResource(R.string.section_grade), modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EnhancePreset.entries.filter { it != EnhancePreset.CUSTOM }.forEach { preset ->
                SwatchCard(
                    label = enhancePresetLabel(preset),
                    selected = state.preset == preset,
                    colors = presetSwatchColors(preset),
                    onClick = { onPreset(preset) },
                )
            }
        }

        SliderRow(
            label = stringResource(R.string.brightness),
            value = state.enhance.brightness,
            valueRange = -1f..1f,
            valueText = formatSigned(state.enhance.brightness),
            isDefault = abs(state.enhance.brightness) < 0.005f,
            onReset = { onEnhance { s -> s.copy(brightness = 0f) } },
            onChange = { value -> onEnhance { s -> s.copy(brightness = value) } },
        )
        SliderRow(
            label = stringResource(R.string.contrast),
            value = state.enhance.contrast,
            valueRange = -1f..1f,
            valueText = formatSigned(state.enhance.contrast),
            isDefault = abs(state.enhance.contrast) < 0.005f,
            onReset = { onEnhance { s -> s.copy(contrast = 0f) } },
            onChange = { value -> onEnhance { s -> s.copy(contrast = value) } },
        )
        SliderRow(
            label = stringResource(R.string.saturation),
            value = state.enhance.saturation,
            valueRange = -1f..1f,
            valueText = formatSigned(state.enhance.saturation),
            isDefault = abs(state.enhance.saturation) < 0.005f,
            onReset = { onEnhance { s -> s.copy(saturation = 0f) } },
            onChange = { value -> onEnhance { s -> s.copy(saturation = value) } },
        )
        SliderRow(
            label = stringResource(R.string.warmth),
            value = state.enhance.warmth,
            valueRange = -1f..1f,
            valueText = formatSigned(state.enhance.warmth),
            isDefault = abs(state.enhance.warmth) < 0.005f,
            onReset = { onEnhance { s -> s.copy(warmth = 0f) } },
            onChange = { value -> onEnhance { s -> s.copy(warmth = value) } },
        )
        SliderRow(
            label = stringResource(R.string.ambient_glow),
            value = state.enhance.glow * 2f - 1f,
            valueRange = -1f..1f,
            valueText = formatPercent(state.enhance.glow),
            isDefault = abs(state.enhance.glow - 0.45f) < 0.005f,
            onReset = { onEnhance { s -> s.copy(glow = 0.45f, enabled = true) } },
            onChange = { value ->
                onEnhance { s ->
                    s.copy(glow = ((value + 1f) / 2f).coerceIn(0f, 1f), enabled = true)
                }
            },
        )

        SectionLabel(stringResource(R.string.section_film), modifier = Modifier.padding(top = 8.dp))
        SliderRow(
            label = stringResource(R.string.film_sharpen),
            value = state.sharpen,
            valueRange = 0f..1f,
            valueText = formatPercent(state.sharpen),
            isDefault = state.sharpen <= 0.005f,
            onReset = { onSharpen(0f) },
            onChange = onSharpen,
        )
        SliderRow(
            label = stringResource(R.string.film_vignette),
            value = state.vignette,
            valueRange = 0f..1f,
            valueText = formatPercent(state.vignette),
            isDefault = state.vignette <= 0.005f,
            onReset = { onVignette(0f) },
            onChange = onVignette,
        )
        SliderRow(
            label = stringResource(R.string.film_grain),
            value = state.grain,
            valueRange = 0f..1f,
            valueText = formatPercent(state.grain),
            isDefault = state.grain <= 0.005f,
            onReset = { onGrain(0f) },
            onChange = onGrain,
        )
    }
}

@Composable
private fun EqPanel(
    loudness: Int,
    bass: Int,
    surround: Int,
    onEqPreset: (EqKind) -> Unit,
    onLoudness: (Int) -> Unit,
    onBass: (Int) -> Unit,
    onSurround: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheetSurface(modifier = modifier) {
        SectionLabel(stringResource(R.string.equalizer))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqChip(stringResource(R.string.eq_normal)) { onEqPreset(EqKind.FLAT) }
            EqChip(stringResource(R.string.eq_bass)) { onEqPreset(EqKind.BASS) }
            EqChip(stringResource(R.string.eq_treble)) { onEqPreset(EqKind.TREBLE) }
            EqChip(stringResource(R.string.eq_vocal)) { onEqPreset(EqKind.VOICE) }
            EqChip(stringResource(R.string.eq_movie)) { onEqPreset(EqKind.MOVIE) }
            EqChip(stringResource(R.string.eq_dialogue)) { onEqPreset(EqKind.DIALOGUE) }
            EqChip(stringResource(R.string.eq_noise_reduce)) { onEqPreset(EqKind.NOISE_REDUCE) }
        }
        SliderRow(
            label = stringResource(R.string.audio_loudness),
            value = loudness.toFloat(),
            valueRange = 0f..2000f,
            valueText = loudness.toString(),
            isDefault = loudness == 0,
            onReset = { onLoudness(0) },
            onChange = { onLoudness(it.roundToInt().coerceIn(0, 2000)) },
        )
        SliderRow(
            label = stringResource(R.string.audio_bass),
            value = bass.toFloat(),
            valueRange = 0f..1000f,
            valueText = bass.toString(),
            isDefault = bass == 0,
            onReset = { onBass(0) },
            onChange = { onBass(it.roundToInt().coerceIn(0, 1000)) },
        )
        SliderRow(
            label = stringResource(R.string.audio_surround),
            value = surround.toFloat(),
            valueRange = 0f..1000f,
            valueText = surround.toString(),
            isDefault = surround == 0,
            onReset = { onSurround(0) },
            onChange = { onSurround(it.roundToInt().coerceIn(0, 1000)) },
        )
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

@Composable
private fun TracksPanel(
    audioTracks: List<String>,
    textTracks: List<String>,
    selectedAudio: Int,
    selectedText: Int,
    subtitleSize: Float,
    subtitlePosition: Float,
    onSelectAudio: (Int) -> Unit,
    onSelectText: (Int) -> Unit,
    onSubtitleSize: (Float) -> Unit,
    onSubtitlePosition: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheetSurface(modifier = modifier) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (audioTracks.isNotEmpty()) {
                SectionLabel(stringResource(R.string.audio_track))
                audioTracks.forEachIndexed { index, label ->
                    TrackRow(label, selectedAudio == index) { onSelectAudio(index) }
                }
            }
            SectionLabel(stringResource(R.string.subtitles), modifier = Modifier.padding(top = 14.dp))
            TrackRow(stringResource(R.string.track_off), selectedText == -1) { onSelectText(-1) }
            textTracks.forEachIndexed { index, label ->
                TrackRow(label, selectedText == index) { onSelectText(index) }
            }
            SliderRow(
                label = stringResource(R.string.subtitle_size),
                value = subtitleSize,
                valueRange = 0.5f..2f,
                valueText = "${subtitleSize}×",
                isDefault = abs(subtitleSize - 1f) < 0.005f,
                onReset = { onSubtitleSize(1f) },
                onChange = onSubtitleSize,
            )
            SliderRow(
                label = stringResource(R.string.subtitle_position),
                value = subtitlePosition,
                valueRange = 0f..0.5f,
                valueText = formatPercent(subtitlePosition),
                isDefault = abs(subtitlePosition - 0.08f) < 0.005f,
                onReset = { onSubtitlePosition(0.08f) },
                onChange = onSubtitlePosition,
            )
        }
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GlowCyan.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) GlowCyan else TextPrimary,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text("✓", color = GlowCyan, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GestureHudOverlay(hud: GestureHud) {
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = when (hud.kind) {
        HudKind.BRIGHTNESS -> Icons.Outlined.Brightness6
        HudKind.VOLUME -> Icons.Outlined.VolumeUp
        HudKind.SEEK -> null
    }
    Box(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(hudAlignment(hud.kind))
                .then(if (hud.kind == HudKind.BRIGHTNESS) Modifier.statusBarsPadding() else Modifier)
                .padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = stringResource(
                        if (hud.kind == HudKind.BRIGHTNESS) R.string.cd_brightness else R.string.cd_volume,
                    ),
                    tint = GlowCyan,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = hud.text,
                color = TextPrimary,
                style = TabularTextStyle.copy(fontSize = 13.sp),
            )
            if (hud.kind != HudKind.SEEK) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hud.progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Brush.horizontalGradient(listOf(GlowCyan, GlowMagenta))),
                    )
                }
            }
        }
    }
}

private fun hudAlignment(kind: HudKind): Alignment = when (kind) {
    HudKind.BRIGHTNESS -> Alignment.TopCenter
    HudKind.VOLUME -> Alignment.CenterEnd
    HudKind.SEEK -> Alignment.Center
}

private val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) "${speed.toLong()}×" else "${speed}×"

fun rotateActivity(activity: Activity) {
    activity.requestedOrientation = if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
