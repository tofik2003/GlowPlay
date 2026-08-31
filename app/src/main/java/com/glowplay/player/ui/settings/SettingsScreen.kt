package com.glowplay.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowplay.player.BuildConfig
import com.glowplay.player.R
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.ui.components.GlowTitle
import com.glowplay.player.ui.components.enhancePresetLabel
import com.glowplay.player.ui.theme.GlowCyan
import com.glowplay.player.ui.theme.Night
import com.glowplay.player.ui.theme.TextSecondary

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
    onImmersive: (Boolean) -> Unit,
    onPreset: (EnhancePreset) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = GlowCyan) }
        GlowTitle(stringResource(R.string.settings))
        Section(stringResource(R.string.settings_playback))
        ToggleRow(stringResource(R.string.remember_position), state.rememberPosition, onRemember)
        ToggleRow(stringResource(R.string.autoplay_next), state.autoplayNext, onAutoplay)
        ToggleRow(stringResource(R.string.hardware_decoder), state.hardwareDecoder, onHardware)
        ToggleRow(stringResource(R.string.pip_on_leave), state.pipOnLeave, onPip)
        ToggleRow(stringResource(R.string.immersive_landscape), state.immersiveLandscape, onImmersive)
        Section(stringResource(R.string.settings_gestures))
        ToggleRow(stringResource(R.string.enable_gestures), state.gesturesEnabled, onGestures)
        ToggleRow(stringResource(R.string.long_press_speed), state.longPressSpeed, onHoldSpeed)
        Section(stringResource(R.string.settings_enhance))
        Text(stringResource(R.string.default_preset), color = TextSecondary, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(EnhancePreset.OFF, EnhancePreset.GLOW, EnhancePreset.CINEMA, EnhancePreset.VIVID).forEach { preset ->
                FilterChip(
                    selected = state.defaultPreset == preset,
                    onClick = { onPreset(preset) },
                    label = { Text(enhancePresetLabel(preset)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Section(stringResource(R.string.about))
        Text(stringResource(R.string.about_body), color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
        Text(
            stringResource(R.string.version_fmt, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            color = TextSecondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = GlowCyan, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = GlowCyan, checkedTrackColor = GlowCyan.copy(alpha = 0.35f)),
        )
    }
}
