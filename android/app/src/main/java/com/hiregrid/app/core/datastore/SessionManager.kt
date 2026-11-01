package com.hiregrid.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hiregrid_session")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
    }

    private val safePreferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                emit(emptyPreferences())
            }
        }

    val accessToken: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_ACCESS_TOKEN]
    }

    val userRole: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_USER_ROLE]
    }

    val userName: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_USER_NAME]
    }

    val userEmail: Flow<String?> = safePreferencesFlow.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    val isLoggedIn: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        !preferences[KEY_ACCESS_TOKEN].isNullOrEmpty()
    }

    suspend fun saveSession(
        token: String,
        userId: String,
        email: String,
        role: String,
        fullName: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = token
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_ROLE] = role.lowercase()
            preferences[KEY_USER_NAME] = fullName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
