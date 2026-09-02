package com.glowplay.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glowplay.player.GlowPlayApp
import com.glowplay.player.data.local.AppPreferences
import com.glowplay.player.data.local.ThemeMode
import com.glowplay.player.data.local.UserPreferences
import com.glowplay.player.enhance.EnhancePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: UserPreferences,
) : ViewModel() {

    val state: StateFlow<AppPreferences> = prefs.flow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppPreferences(),
    )

    fun setRemember(value: Boolean) = launch { prefs.setRememberPosition(value) }
    fun setAutoplay(value: Boolean) = launch { prefs.setAutoplayNext(value) }
    fun setGestures(value: Boolean) = launch { prefs.setGestures(value) }
    fun setHoldSpeed(value: Boolean) = launch { prefs.setLongPressSpeed(value) }
    fun setHardware(value: Boolean) = launch { prefs.setHardwareDecoder(value) }
    fun setPip(value: Boolean) = launch { prefs.setPipOnLeave(value) }
    fun setPreset(value: EnhancePreset) = launch { prefs.setPreset(value) }
    fun setThemeMode(value: ThemeMode) = launch { prefs.setThemeMode(value) }
    fun setSeekStep(value: Int) = launch { prefs.setSeekStepSeconds(value) }
    fun setHoldSpeedValue(value: Float) = launch { prefs.setHoldSpeedValue(value) }
    fun setSubtitleScale(value: Float) = launch { prefs.setSubtitleScale(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlowPlayApp
                SettingsViewModel(app.prefs)
            }
        }
    }
}
