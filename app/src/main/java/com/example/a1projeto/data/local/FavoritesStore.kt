package com.example.a1projeto.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class FavKind { LIVE, VOD, SERIES }

object FavoritesStore {

    private val KEY_FAVORITES = stringSetPreferencesKey("favorites_ids")

    private fun token(kind: FavKind, id: Int) = "${kind.name}:$id"

    fun favoritesFlow(context: Context, kind: FavKind): Flow<Set<Int>> {
        return context.settingsDataStore.data.map { prefs ->
            val raw = prefs[KEY_FAVORITES] ?: emptySet()
            raw.mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2 && parts[0] == kind.name) parts[1].toIntOrNull() else null
            }.toSet()
        }
    }

    suspend fun toggleFavorite(context: Context, kind: FavKind, id: Int) {
        context.settingsDataStore.edit { prefs ->
            val set = (prefs[KEY_FAVORITES] ?: emptySet()).toMutableSet()
            val t = token(kind, id)
            if (set.contains(t)) set.remove(t) else set.add(t)
            prefs[KEY_FAVORITES] = set
        }
    }
}
