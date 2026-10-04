package com.promptstudio.app.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferences by preferencesDataStore(name = USER_PREFERENCES_NAME)
private const val USER_PREFERENCES_NAME = "user_preferences"

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val downloadFolderUri: String? = null,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val preferences: Flow<UserPreferences> = context.userPreferences.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map(::mapPreferences)

    suspend fun setTheme(theme: ThemePreference) {
        context.userPreferences.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setDownloadFolderUri(uri: String?) {
        context.userPreferences.edit { preferences ->
            if (uri == null) preferences.remove(Keys.downloadFolderUri)
            else preferences[Keys.downloadFolderUri] = uri
        }
    }

    private fun mapPreferences(preferences: Preferences) = UserPreferences(
        theme = preferences[Keys.theme]
            ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
            ?: ThemePreference.SYSTEM,
        downloadFolderUri = preferences[Keys.downloadFolderUri],
    )

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val downloadFolderUri = stringPreferencesKey("download_folder_uri")
    }
}
