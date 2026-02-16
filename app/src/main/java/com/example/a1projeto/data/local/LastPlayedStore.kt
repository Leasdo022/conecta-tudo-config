package com.example.a1projeto.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lastPlayedDataStore by preferencesDataStore(name = "last_played")

class LastPlayedStore(private val context: Context) {

    private val LAST_ID = stringPreferencesKey("last_id")
    private val LAST_TITLE = stringPreferencesKey("last_title")
    private val LAST_KIND = stringPreferencesKey("last_kind") // channel | episode | vod
    private val LAST_POSITION = longPreferencesKey("last_position")
    private val LAST_DURATION = longPreferencesKey("last_duration")

    val lastId: Flow<String?> = context.lastPlayedDataStore.data.map { it[LAST_ID] }
    val lastTitle: Flow<String?> = context.lastPlayedDataStore.data.map { it[LAST_TITLE] }
    val lastKind: Flow<String?> = context.lastPlayedDataStore.data.map { it[LAST_KIND] }
    val lastPosition: Flow<Long?> = context.lastPlayedDataStore.data.map { it[LAST_POSITION] }
    val lastDuration: Flow<Long?> = context.lastPlayedDataStore.data.map { it[LAST_DURATION] }

    suspend fun save(id: String, title: String, kind: String, position: Long) {
        context.lastPlayedDataStore.edit { prefs ->
            prefs[LAST_ID] = id
            prefs[LAST_TITLE] = title
            prefs[LAST_KIND] = kind
            prefs[LAST_POSITION] = position
        }
    }

    suspend fun save(id: String, title: String, kind: String, position: Long, duration: Long) {
        context.lastPlayedDataStore.edit { prefs ->
            prefs[LAST_ID] = id
            prefs[LAST_TITLE] = title
            prefs[LAST_KIND] = kind
            prefs[LAST_POSITION] = position
            prefs[LAST_DURATION] = duration
        }
    }

    suspend fun save(id: String, title: String, kind: String) {
        save(id, title, kind, position = 0L)
    }

    suspend fun clear() {
        context.lastPlayedDataStore.edit { prefs ->
            prefs.remove(LAST_ID)
            prefs.remove(LAST_TITLE)
            prefs.remove(LAST_KIND)
            prefs.remove(LAST_POSITION)
            prefs.remove(LAST_DURATION)
        }
    }
}
