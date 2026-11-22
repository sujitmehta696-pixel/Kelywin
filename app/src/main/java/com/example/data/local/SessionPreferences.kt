package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.shared.model.GameMode
import com.example.shared.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kelywin_session")

data class RememberedCredentials(
    val remember: Boolean = true,
    val countryCode: String = "+91",
    val identifier: String = "",
    val password: String = ""
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val REMEMBER_PASSWORD = booleanPreferencesKey("remember_password")
        val SAVED_COUNTRY_CODE = stringPreferencesKey("saved_country_code")
        val SAVED_IDENTIFIER = stringPreferencesKey("saved_identifier")
        val SAVED_PASSWORD = stringPreferencesKey("saved_password")
        val ACTIVE_GAME_MODE = stringPreferencesKey("active_game_mode")
        val SHOW_RESULT_POPUP = booleanPreferencesKey("show_result_popup")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val COMPACT_GRID = booleanPreferencesKey("compact_grid")
    }

    val activeUserIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_USER_ID]
    }

    val rememberedCredentialsFlow: Flow<RememberedCredentials> = context.dataStore.data.map { prefs ->
        RememberedCredentials(
            remember = prefs[Keys.REMEMBER_PASSWORD] ?: true,
            countryCode = prefs[Keys.SAVED_COUNTRY_CODE] ?: "+91",
            identifier = prefs[Keys.SAVED_IDENTIFIER] ?: "",
            password = prefs[Keys.SAVED_PASSWORD] ?: ""
        )
    }

    val activeGameModeFlow: Flow<GameMode> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.ACTIVE_GAME_MODE] ?: GameMode.SEC_30.id
        GameMode.entries.find { it.id == id } ?: GameMode.SEC_30
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            showResultPopup = prefs[Keys.SHOW_RESULT_POPUP] ?: true,
            soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS] ?: true,
            compactGridMode = prefs[Keys.COMPACT_GRID] ?: false
        )
    }

    suspend fun setActiveUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.ACTIVE_USER_ID)
            } else {
                prefs[Keys.ACTIVE_USER_ID] = userId
            }
        }
    }

    suspend fun saveRememberedCredentials(
        remember: Boolean,
        countryCode: String,
        identifier: String,
        password: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMEMBER_PASSWORD] = remember
            if (remember) {
                prefs[Keys.SAVED_COUNTRY_CODE] = countryCode
                prefs[Keys.SAVED_IDENTIFIER] = identifier
                prefs[Keys.SAVED_PASSWORD] = password
            } else {
                prefs.remove(Keys.SAVED_IDENTIFIER)
                prefs.remove(Keys.SAVED_PASSWORD)
            }
        }
    }

    suspend fun setActiveGameMode(mode: GameMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_GAME_MODE] = mode.id
        }
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_RESULT_POPUP] = settings.showResultPopup
            prefs[Keys.SOUND_EFFECTS] = settings.soundEffectsEnabled
            prefs[Keys.COMPACT_GRID] = settings.compactGridMode
        }
    }
}
