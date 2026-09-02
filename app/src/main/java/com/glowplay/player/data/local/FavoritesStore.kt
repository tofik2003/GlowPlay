package com.glowplay.player.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "glowplay_favorites")

/**
 * Persists the set of "favorited" video media keys so the library can show a
 * Favorites tab and a filled-heart badge, independent from MediaStore (which
 * has no concept of app-level favorites).
 */
class FavoritesStore(context: Context) {
    private val dataStore = context.applicationContext.favoritesDataStore

    val favorites: Flow<Set<String>> = dataStore.data.map { it[KEY].orEmpty() }

    suspend fun toggle(mediaKey: String) {
        if (mediaKey.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[KEY].orEmpty().toMutableSet()
            if (!current.add(mediaKey)) current.remove(mediaKey)
            prefs[KEY] = current
        }
    }

    suspend fun remove(mediaKeys: Collection<String>) {
        if (mediaKeys.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[KEY].orEmpty().toMutableSet()
            current.removeAll(mediaKeys.toSet())
            prefs[KEY] = current
        }
    }

    companion object {
        private val KEY = stringSetPreferencesKey("favorite_keys")
    }
}
