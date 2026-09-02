package com.glowplay.player.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.VideoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userStore by preferencesDataStore(name = "glowplay_user")

enum class ThemeMode(val storageKey: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: LIGHT
    }
}

data class AppPreferences(
    val rememberPosition: Boolean = true,
    val autoplayNext: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val longPressSpeed: Boolean = true,
    val hardwareDecoder: Boolean = true,
    val pipOnLeave: Boolean = true,
    val defaultPreset: EnhancePreset = EnhancePreset.GLOW,
    val sort: VideoSort = VideoSort.DATE_NEW,
    val customEnhance: EnhanceSettings = EnhanceSettings(enabled = true),
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val seekStepSeconds: Int = 10,
    val holdSpeedValue: Float = 2f,
)

class UserPreferences(context: Context) {
    private val dataStore = context.applicationContext.userStore

    val flow: Flow<AppPreferences> = dataStore.data.map { it.toPrefs() }

    suspend fun setRememberPosition(value: Boolean) = set(Keys.remember, value)
    suspend fun setAutoplayNext(value: Boolean) = set(Keys.autoplay, value)
    suspend fun setGestures(value: Boolean) = set(Keys.gestures, value)
    suspend fun setLongPressSpeed(value: Boolean) = set(Keys.holdSpeed, value)
    suspend fun setHardwareDecoder(value: Boolean) = set(Keys.hw, value)
    suspend fun setPipOnLeave(value: Boolean) = set(Keys.pip, value)

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.storageKey }
    }

    suspend fun setSeekStepSeconds(seconds: Int) {
        dataStore.edit { it[Keys.seekStep] = seconds.coerceIn(5, 60) }
    }

    suspend fun setHoldSpeedValue(speed: Float) {
        dataStore.edit { it[Keys.holdSpeedValue] = speed.coerceIn(1.25f, 4f) }
    }

    suspend fun setPreset(preset: EnhancePreset) {
        dataStore.edit { it[Keys.preset] = preset.storageKey }
    }

    suspend fun setSort(sort: VideoSort) {
        dataStore.edit { it[Keys.sort] = sort.name }
    }

    suspend fun setCustomEnhance(settings: EnhanceSettings) {
        val s = settings.clamped()
        dataStore.edit {
            it[Keys.custom] = listOf(
                s.brightness,
                s.contrast,
                s.saturation,
                s.warmth,
                s.hue,
                s.tint,
                s.glow,
            ).joinToString(",")
        }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toPrefs(): AppPreferences {
        val customParts = this[Keys.custom]?.split(",")?.mapNotNull { it.toFloatOrNull() }.orEmpty()
        val custom = when {
            // v2 format: brightness, contrast, saturation, warmth, hue, tint, glow
            customParts.size >= 7 -> EnhanceSettings(
                brightness = customParts[0],
                contrast = customParts[1],
                saturation = customParts[2],
                warmth = customParts[3],
                hue = customParts[4],
                tint = customParts[5],
                glow = customParts[6],
                enabled = true,
            ).clamped()
            // v1 legacy format: brightness, contrast, saturation, warmth, glow
            customParts.size >= 5 -> EnhanceSettings(
                brightness = customParts[0],
                contrast = customParts[1],
                saturation = customParts[2],
                warmth = customParts[3],
                glow = customParts[4],
                enabled = true,
            ).clamped()
            else -> EnhanceSettings(enabled = true)
        }
        val sort = runCatching { VideoSort.valueOf(this[Keys.sort] ?: VideoSort.DATE_NEW.name) }
            .getOrDefault(VideoSort.DATE_NEW)
        return AppPreferences(
            rememberPosition = this[Keys.remember] ?: true,
            autoplayNext = this[Keys.autoplay] ?: true,
            gesturesEnabled = this[Keys.gestures] ?: true,
            longPressSpeed = this[Keys.holdSpeed] ?: true,
            hardwareDecoder = this[Keys.hw] ?: true,
            pipOnLeave = this[Keys.pip] ?: true,
            defaultPreset = EnhancePreset.fromKey(this[Keys.preset]),
            sort = sort,
            customEnhance = custom,
            themeMode = ThemeMode.fromKey(this[Keys.theme]),
            seekStepSeconds = (this[Keys.seekStep] ?: 10).coerceIn(5, 60),
            holdSpeedValue = (this[Keys.holdSpeedValue] ?: 2f).coerceIn(1.25f, 4f),
        )
    }

    private object Keys {
        val remember = booleanPreferencesKey("remember_position")
        val autoplay = booleanPreferencesKey("autoplay_next")
        val gestures = booleanPreferencesKey("gestures")
        val holdSpeed = booleanPreferencesKey("hold_speed")
        val hw = booleanPreferencesKey("hw_decoder")
        val pip = booleanPreferencesKey("pip_on_leave")
        val preset = stringPreferencesKey("preset")
        val sort = stringPreferencesKey("sort")
        val custom = stringPreferencesKey("custom_enhance")
        val theme = stringPreferencesKey("theme_mode")
        val seekStep = intPreferencesKey("seek_step_seconds")
        val holdSpeedValue = floatPreferencesKey("hold_speed_value")
    }
}
