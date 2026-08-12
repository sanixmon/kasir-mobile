package com.kasir.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kasir_prefs")

data class SessionData(
    val serverUrl: String,
    val currentUserId: String,
    val currentUserName: String,
    val isAdmin: Boolean
)

class SessionManager(private val context: Context) {
    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val CURRENT_USER_ID_KEY = stringPreferencesKey("current_user_id")
        val CURRENT_USER_NAME_KEY = stringPreferencesKey("current_user_name")
        val IS_ADMIN_KEY = booleanPreferencesKey("is_admin")
    }

    val sessionDataFlow: Flow<SessionData> = context.dataStore.data.map { prefs ->
        SessionData(
            serverUrl = prefs[SERVER_URL_KEY] ?: "http://10.0.2.2:3001",
            currentUserId = prefs[CURRENT_USER_ID_KEY] ?: "",
            currentUserName = prefs[CURRENT_USER_NAME_KEY] ?: "",
            isAdmin = prefs[IS_ADMIN_KEY] ?: false
        )
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL_KEY] = url }
    }

    suspend fun setCurrentUser(id: String, name: String) {
        context.dataStore.edit { 
            it[CURRENT_USER_ID_KEY] = id
            it[CURRENT_USER_NAME_KEY] = name
        }
    }

    suspend fun setAdminStatus(isAdmin: Boolean) {
        context.dataStore.edit { it[IS_ADMIN_KEY] = isAdmin }
    }
    
    suspend fun clearSession() {
        context.dataStore.edit { 
            it.remove(CURRENT_USER_ID_KEY)
            it.remove(CURRENT_USER_NAME_KEY)
            it.remove(IS_ADMIN_KEY)
        }
    }
}
