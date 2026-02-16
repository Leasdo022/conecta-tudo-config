package com.example.a1projeto.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AuthState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val expDateSeconds: Long = 0L,
    val maxConnections: Int = 0,
    val activeConnections: Int = 0
) {
    val isLoggedIn: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

object AuthStore {

    private val KEY_URL = stringPreferencesKey("server_url")
    private val KEY_USER = stringPreferencesKey("username")
    private val KEY_PASS = stringPreferencesKey("password")

    private val KEY_EXP = longPreferencesKey("exp_date_seconds")
    private val KEY_MAX = intPreferencesKey("max_connections")
    private val KEY_ACTIVE = intPreferencesKey("active_connections")

    fun authFlow(context: Context): Flow<AuthState> {
        return context.dataStore.data.map { prefs ->
            AuthState(
                serverUrl = prefs[KEY_URL] ?: "",
                username = prefs[KEY_USER] ?: "",
                password = prefs[KEY_PASS] ?: "",
                expDateSeconds = prefs[KEY_EXP] ?: 0L,
                maxConnections = prefs[KEY_MAX] ?: 0,
                activeConnections = prefs[KEY_ACTIVE] ?: 0
            )
        }
    }

    suspend fun save(
        context: Context,
        serverUrl: String,
        username: String,
        password: String,
        expDateSeconds: Long = 0L,
        maxConnections: Int = 0,
        activeConnections: Int = 0
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_URL] = serverUrl
            prefs[KEY_USER] = username
            prefs[KEY_PASS] = password
            prefs[KEY_EXP] = expDateSeconds
            prefs[KEY_MAX] = maxConnections
            prefs[KEY_ACTIVE] = activeConnections
        }
    }

    suspend fun updateServerUrl(context: Context, serverUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_URL] = serverUrl
        }
    }

    suspend fun logout(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_URL)
            prefs.remove(KEY_USER)
            prefs.remove(KEY_PASS)
            prefs.remove(KEY_EXP)
            prefs.remove(KEY_MAX)
            prefs.remove(KEY_ACTIVE)
        }
    }
}
