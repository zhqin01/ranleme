package com.zendrive.simulator.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {

    companion object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")        // "dark" | "light" | "auto"
        val LAST_SCENE = stringPreferencesKey("last_scene")         // DriveScene enum name
        val TTS_VOLUME = floatPreferencesKey("tts_volume")          // 0.0 .. 1.0
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val COINS = intPreferencesKey("coins")
        val ADMIN_MODE = booleanPreferencesKey("admin_mode")
        val ORDER_MODE = stringPreferencesKey("order_mode")  // "auto" | "bubble"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "auto" }
    val lastScene: Flow<String> = context.dataStore.data.map { it[LAST_SCENE] ?: "" }
    val ttsVolume: Flow<Float> = context.dataStore.data.map { it[TTS_VOLUME] ?: 0.8f }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[FIRST_LAUNCH] ?: true }
    val coins: Flow<Int> = context.dataStore.data.map { it[COINS] ?: 0 }
    val isAdminMode: Flow<Boolean> = context.dataStore.data.map { it[ADMIN_MODE] ?: false }
    val orderMode: Flow<String> = context.dataStore.data.map { it[ORDER_MODE] ?: "auto" }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[COINS] ?: 0
            prefs[COINS] = current + amount
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val current = prefs[COINS] ?: 0
            if (current >= amount) {
                prefs[COINS] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { it[THEME_MODE] = value }
    }

    suspend fun setLastScene(value: String) {
        context.dataStore.edit { it[LAST_SCENE] = value }
    }

    suspend fun setTtsVolume(value: Float) {
        context.dataStore.edit { it[TTS_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setSoundEnabled(value: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = value }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = value }
    }

    suspend fun setAdminMode(enabled: Boolean) {
        context.dataStore.edit { it[ADMIN_MODE] = enabled }
    }

    suspend fun setOrderMode(mode: String) {
        context.dataStore.edit { it[ORDER_MODE] = mode }
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[FIRST_LAUNCH] = false }
    }
}
