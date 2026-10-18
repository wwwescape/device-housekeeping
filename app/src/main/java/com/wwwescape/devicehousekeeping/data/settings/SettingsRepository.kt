package com.wwwescape.devicehousekeeping.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Mirrors [ThemeMode] outside DataStore, which is Flow-only/async. [MainActivity] reads this
 * synchronously in `attachBaseContext`, before any Compose or DataStore code can run, so the
 * window's initial (pre-Compose) theme resolution honors the user's choice instead of always
 * following the raw system day/night setting. */
const val THEME_PREFS_NAME = "theme_prefs_sync"
const val THEME_MODE_PREF_KEY = "theme_mode"

object SettingsRepository {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val COLOR_THEME_KEY = stringPreferencesKey("color_theme")
    private val THEME_CONTRAST_KEY = stringPreferencesKey("theme_contrast")
    private val PURE_DARK_KEY = booleanPreferencesKey("pure_dark")
    private val ABSOLUTE_DARK_KEY = booleanPreferencesKey("absolute_dark")
    private val MIN_FILE_AGE_KEY = stringPreferencesKey("min_file_age")
    private val CONFIRM_CLEANING_KEY = booleanPreferencesKey("confirm_before_cleaning")
    private val SCAN_REMOVABLE_KEY = booleanPreferencesKey("scan_removable_storage")
    private val EXCLUDED_PATHS_KEY = stringSetPreferencesKey("excluded_paths")
    private val AUTO_CLEAN_KEY = stringPreferencesKey("auto_clean")

    fun settingsFlow(context: Context): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME_MODE_KEY].toEnumOrDefault(ThemeMode.SYSTEM),
            useDynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            colorTheme = prefs[COLOR_THEME_KEY].toEnumOrDefault(ColorTheme.DEFAULT),
            themeContrast = prefs[THEME_CONTRAST_KEY].toEnumOrDefault(ThemeContrast.STANDARD),
            pureDark = prefs[PURE_DARK_KEY] ?: false,
            absoluteDark = prefs[ABSOLUTE_DARK_KEY] ?: false,
            cleaning = CleaningSettings(
                minFileAge = prefs[MIN_FILE_AGE_KEY].toEnumOrDefault(MinFileAge.ONE_DAY),
                autoClean = prefs[AUTO_CLEAN_KEY].toEnumOrDefault(AutoCleanInterval.OFF),
                confirmBeforeCleaning = prefs[CONFIRM_CLEANING_KEY] ?: true,
                scanRemovableStorage = prefs[SCAN_REMOVABLE_KEY] ?: true,
                excludedPaths = prefs[EXCLUDED_PATHS_KEY] ?: emptySet(),
            ),
        )
    }

    suspend fun current(context: Context): AppSettings = settingsFlow(context).first()

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
        context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(THEME_MODE_PREF_KEY, mode.name).apply()
    }

    suspend fun setDynamicColor(context: Context, enabled: Boolean) = put(context, DYNAMIC_COLOR_KEY, enabled)
    suspend fun setColorTheme(context: Context, theme: ColorTheme) = put(context, COLOR_THEME_KEY, theme.name)
    suspend fun setThemeContrast(context: Context, contrast: ThemeContrast) = put(context, THEME_CONTRAST_KEY, contrast.name)
    suspend fun setPureDark(context: Context, enabled: Boolean) = put(context, PURE_DARK_KEY, enabled)
    suspend fun setAbsoluteDark(context: Context, enabled: Boolean) = put(context, ABSOLUTE_DARK_KEY, enabled)
    suspend fun setMinFileAge(context: Context, age: MinFileAge) = put(context, MIN_FILE_AGE_KEY, age.name)
    suspend fun setAutoClean(context: Context, interval: AutoCleanInterval) = put(context, AUTO_CLEAN_KEY, interval.name)
    suspend fun setConfirmBeforeCleaning(context: Context, enabled: Boolean) = put(context, CONFIRM_CLEANING_KEY, enabled)
    suspend fun setScanRemovableStorage(context: Context, enabled: Boolean) = put(context, SCAN_REMOVABLE_KEY, enabled)

    suspend fun addExcludedPath(context: Context, path: String) {
        context.settingsDataStore.edit { it[EXCLUDED_PATHS_KEY] = (it[EXCLUDED_PATHS_KEY] ?: emptySet()) + path }
    }

    suspend fun removeExcludedPath(context: Context, path: String) {
        context.settingsDataStore.edit { it[EXCLUDED_PATHS_KEY] = (it[EXCLUDED_PATHS_KEY] ?: emptySet()) - path }
    }

    private suspend fun <T> put(context: Context, key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: default
}
