package com.wwwescape.devicehousekeeping.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.devicehousekeeping.data.schedule.AutoCleanScheduler
import com.wwwescape.devicehousekeeping.data.settings.AppSettings
import com.wwwescape.devicehousekeeping.data.settings.AutoCleanInterval
import kotlinx.coroutines.Dispatchers
import com.wwwescape.devicehousekeeping.data.settings.ColorTheme
import com.wwwescape.devicehousekeeping.data.settings.MinFileAge
import com.wwwescape.devicehousekeeping.data.settings.SettingsRepository
import com.wwwescape.devicehousekeeping.data.settings.ThemeContrast
import com.wwwescape.devicehousekeeping.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = SettingsRepository.settingsFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private fun save(block: suspend (Context) -> Unit) {
        viewModelScope.launch { block(getApplication()) }
    }

    fun setThemeMode(mode: ThemeMode) = save { SettingsRepository.setThemeMode(it, mode) }
    fun setDynamicColor(enabled: Boolean) = save { SettingsRepository.setDynamicColor(it, enabled) }
    fun setColorTheme(theme: ColorTheme) = save { SettingsRepository.setColorTheme(it, theme) }
    fun setThemeContrast(contrast: ThemeContrast) = save { SettingsRepository.setThemeContrast(it, contrast) }
    fun setPureDark(enabled: Boolean) = save { SettingsRepository.setPureDark(it, enabled) }
    fun setAbsoluteDark(enabled: Boolean) = save { SettingsRepository.setAbsoluteDark(it, enabled) }
    fun setMinFileAge(age: MinFileAge) = save { SettingsRepository.setMinFileAge(it, age) }

    fun setAutoClean(interval: AutoCleanInterval) {
        viewModelScope.launch(Dispatchers.IO) {
            SettingsRepository.setAutoClean(getApplication(), interval)
            AutoCleanScheduler.apply(getApplication(), interval)
        }
    }
    fun setConfirmBeforeCleaning(enabled: Boolean) = save { SettingsRepository.setConfirmBeforeCleaning(it, enabled) }
    fun setScanRemovableStorage(enabled: Boolean) = save { SettingsRepository.setScanRemovableStorage(it, enabled) }
    fun removeExcludedPath(path: String) = save { SettingsRepository.removeExcludedPath(it, path) }
}
