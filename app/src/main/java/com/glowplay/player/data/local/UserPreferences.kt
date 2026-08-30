package com.glowplay.player.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glowplay.player.data.model.EnhanceSettings
import com.glowplay.player.enhance.EnhancePreset
import com.glowplay.player.enhance.VideoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userStore by preferencesDataStore(name = "glowplay_user")

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
    val loudness: Int = 0,
    val bassBoost: Int = 0,
    val surround: Int = 0,
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

    suspend fun setLoudness(value: Int) = setInt(Keys.loudness, value)
    suspend fun setBass(value: Int) = setInt(Keys.bass, value)
    suspend fun setSurround(value: Int) = setInt(Keys.surround, value)

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
                s.glow,
            ).joinToString(",")
        }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun setInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toPrefs(): AppPreferences {
        val customParts = this[Keys.custom]?.split(",")?.mapNotNull { it.toFloatOrNull() }.orEmpty()
        val custom = if (customParts.size >= 5) {
            EnhanceSettings(
                brightness = customParts[0],
                contrast = customParts[1],
                saturation = customParts[2],
                warmth = customParts[3],
                glow = customParts[4],
                enabled = true,
            ).clamped()
        } else {
            EnhanceSettings(enabled = true)
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
            loudness = (this[Keys.loudness] ?: 0).coerceIn(0, 2000),
            bassBoost = (this[Keys.bass] ?: 0).coerceIn(0, 1000),
            surround = (this[Keys.surround] ?: 0).coerceIn(0, 1000),
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
        val loudness = intPreferencesKey("audio_loudness")
        val bass = intPreferencesKey("audio_bass")
        val surround = intPreferencesKey("audio_surround")
    }
}
