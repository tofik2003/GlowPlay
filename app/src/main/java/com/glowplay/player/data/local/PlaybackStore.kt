package com.glowplay.player.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playbackStore by preferencesDataStore(name = "glowplay_resume")

class PlaybackStore(context: Context) {
    private val dataStore = context.applicationContext.playbackStore

    val positions: Flow<Map<String, Long>> = dataStore.data.map { prefs ->
        parse(prefs[KEY] ?: "")
    }

    suspend fun save(mediaKey: String, positionMs: Long) {
        if (mediaKey.isBlank()) return
        dataStore.edit { prefs ->
            val map = parse(prefs[KEY] ?: "").toMutableMap()
            if (positionMs <= 0L) {
                map.remove(mediaKey)
            } else {
                map[mediaKey] = positionMs
                while (map.size > MAX_ENTRIES) {
                    val oldest = map.keys.firstOrNull() ?: break
                    map.remove(oldest)
                }
            }
            prefs[KEY] = serialize(map)
        }
    }

    suspend fun get(mediaKey: String): Long = positions.first()[mediaKey] ?: 0L

    suspend fun clear(mediaKey: String) = save(mediaKey, 0L)

    companion object {
        private val KEY = stringPreferencesKey("resume_csv")
        private const val MAX_ENTRIES = 400

        internal fun parse(raw: String): Map<String, Long> {
            if (raw.isBlank()) return emptyMap()
            val out = LinkedHashMap<String, Long>()
            raw.split('\n').forEach { line ->
                val idx = line.lastIndexOf('|')
                if (idx <= 0) return@forEach
                val key = line.substring(0, idx)
                val value = line.substring(idx + 1).toLongOrNull() ?: return@forEach
                if (key.isNotBlank() && value > 0L) out[key] = value
            }
            return out
        }

        internal fun serialize(map: Map<String, Long>): String =
            map.entries.joinToString("\n") { "${it.key}|${it.value}" }
    }
}
