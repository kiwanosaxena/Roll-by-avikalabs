package com.avikalabs.roll.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    DAY,
    NIGHT
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "roll_settings")

class RollPreferences(private val context: Context) {

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_HOME_DOUBLE_TAP_ENABLED = booleanPreferencesKey("home_double_tap_enabled")
    private val KEY_IS_PRO = booleanPreferencesKey("is_pro")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val homeDoubleTapEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HOME_DOUBLE_TAP_ENABLED] ?: false
    }

    val isProFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_PRO] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setHomeDoubleTapEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_DOUBLE_TAP_ENABLED] = enabled
        }
    }

    suspend fun setIsPro(isPro: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_PRO] = isPro
        }
        try {
            com.avikalabs.roll.widget.RollWidgetProvider.refreshAllWidgets(context, isPro)
        } catch (_: Exception) {
        }
    }

    fun isProSync(): Boolean {
        return try {
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                context.dataStore.data.first()[KEY_IS_PRO] ?: false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isHomeDoubleTapEnabledSync(): Boolean {
        return try {
            context.dataStore.data.first()[KEY_HOME_DOUBLE_TAP_ENABLED] ?: false
        } catch (_: Exception) {
            false
        }
    }
}
