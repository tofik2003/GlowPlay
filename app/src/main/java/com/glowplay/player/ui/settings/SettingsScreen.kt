package com.glowplay.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowplay.player.BuildConfig
import com.glowplay.player.R
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.data.local.ThemeMode
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.ui.components.NeonCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: AppPreferences,
    onBack: () -> Unit,
    onRemember: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit,
    onGestures: (Boolean) -> Unit,
    onHoldSpeed: (Boolean) -> Unit,
    onHardware: (Boolean) -> Unit,
    onPip: (Boolean) -> Unit,
    onPreset: (EnhancePreset) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onSeekStep: (Int) -> Unit,
    onHoldSpeedValue: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.settings),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Appearance ───────────────────────────────────────────────────────
        SettingsCard(icon = Icons.Outlined.Palette, title = stringResource(R.string.settings_appearance)) {
            Text(
                stringResource(R.string.theme_mode),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeChip(stringResource(R.string.theme_light), state.themeMode == ThemeMode.LIGHT) { onThemeMode(ThemeMode.LIGHT) }
                ThemeChip(stringResource(R.string.theme_dark), state.themeMode == ThemeMode.DARK) { onThemeMode(ThemeMode.DARK) }
                ThemeChip(stringResource(R.string.theme_system), state.themeMode == ThemeMode.SYSTEM) { onThemeMode(ThemeMode.SYSTEM) }
            }
        }

        // ── Playback ─────────────────────────────────────────────────────────
        SettingsCard(icon = Icons.Outlined.PlayCircleOutline, title = stringResource(R.string.settings_playback)) {
            ToggleRow(stringResource(R.string.remember_position), state.rememberPosition, onRemember)
            ToggleRow(stringResource(R.string.autoplay_next), state.autoplayNext, onAutoplay)
            ToggleRow(stringResource(R.string.hardware_decoder), state.hardwareDecoder, onHardware)
            ToggleRow(stringResource(R.string.pip_on_leave), state.pipOnLeave, onPip)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.seek_step),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 10, 15, 30).forEach { step ->
                    ThemeChip("${step}s", state.seekStepSeconds == step) { onSeekStep(step) }
                }
            }
        }

        // ── Gestures ─────────────────────────────────────────────────────────
        SettingsCard(icon = Icons.Outlined.Gesture, title = stringResource(R.string.settings_gestures)) {
            ToggleRow(stringResource(R.string.enable_gestures), state.gesturesEnabled, onGestures)
            ToggleRow(stringResource(R.string.long_press_speed), state.longPressSpeed, onHoldSpeed)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.hold_speed_value),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1.5f, 2f, 3f).forEach { speed ->
                    ThemeChip("${speed}×", state.holdSpeedValue == speed) { onHoldSpeedValue(speed) }
                }
            }
        }

        // ── GlowEnhance ──────────────────────────────────────────────────────
        SettingsCard(icon = Icons.Outlined.AutoAwesome, title = stringResource(R.string.settings_enhance)) {
            Text(
                stringResource(R.string.default_preset),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EnhancePreset.entries.filter { it != EnhancePreset.CUSTOM }.forEach { preset ->
                    ThemeChip(presetLabel(preset), state.defaultPreset == preset) { onPreset(preset) }
                }
            }
        }

        // ── About ────────────────────────────────────────────────────────────
        SettingsCard(icon = Icons.Outlined.Info, title = stringResource(R.string.about)) {
            Text(
                stringResource(R.string.about_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.version_fmt, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
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
