package com.wwwescape.devicehousekeeping

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wwwescape.devicehousekeeping.data.settings.AppSettings
import com.wwwescape.devicehousekeeping.data.settings.SettingsRepository
import com.wwwescape.devicehousekeeping.data.settings.THEME_MODE_PREF_KEY
import com.wwwescape.devicehousekeeping.data.settings.THEME_PREFS_NAME
import com.wwwescape.devicehousekeeping.data.settings.ThemeMode
import com.wwwescape.devicehousekeeping.ui.DeviceHousekeepingApp
import com.wwwescape.devicehousekeeping.ui.theme.DeviceHousekeepingTheme

class MainActivity : ComponentActivity() {

    /** Runs before any DataStore/Compose code can — reads the synchronous SharedPreferences
     * mirror of [ThemeMode] (see [SettingsRepository.setThemeMode]) and forces the base context's
     * night-mode configuration to match, so the window's initial (pre-Compose) theme resolution
     * honors the user's choice instead of always following the raw system day/night setting. */
    override fun attachBaseContext(newBase: Context) {
        val stored = newBase.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(THEME_MODE_PREF_KEY, null)
        val mode = stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
        val forcedNightBit = when (mode) {
            ThemeMode.LIGHT -> Configuration.UI_MODE_NIGHT_NO
            ThemeMode.DARK -> Configuration.UI_MODE_NIGHT_YES
            ThemeMode.SYSTEM -> null
        }
        val context = if (forcedNightBit == null) {
            newBase
        } else {
            val overridden = Configuration(newBase.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or forcedNightBit
            }
            newBase.createConfigurationContext(overridden)
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by remember { SettingsRepository.settingsFlow(applicationContext) }
                .collectAsState(initial = AppSettings())
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            DeviceHousekeepingTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor,
                colorTheme = settings.colorTheme,
                themeContrast = settings.themeContrast,
                pureDark = settings.pureDark,
                absoluteDark = settings.absoluteDark,
            ) {
                DeviceHousekeepingApp()
            }
        }
    }
}
