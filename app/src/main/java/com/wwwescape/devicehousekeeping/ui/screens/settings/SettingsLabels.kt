package com.wwwescape.devicehousekeeping.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.settings.AutoCleanInterval
import com.wwwescape.devicehousekeeping.data.settings.ColorTheme
import com.wwwescape.devicehousekeeping.data.settings.MinFileAge
import com.wwwescape.devicehousekeeping.data.settings.ThemeContrast
import com.wwwescape.devicehousekeeping.data.settings.ThemeMode

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.SYSTEM -> R.string.theme_mode_system
    },
)

@Composable
fun ColorTheme.label(): String = stringResource(
    when (this) {
        ColorTheme.DEFAULT -> R.string.color_theme_default
        ColorTheme.OCEAN -> R.string.color_theme_ocean
        ColorTheme.FOREST -> R.string.color_theme_forest
        ColorTheme.SUNSET -> R.string.color_theme_sunset
        ColorTheme.GRAPE -> R.string.color_theme_grape
        ColorTheme.ROSE -> R.string.color_theme_rose
        ColorTheme.SLATE -> R.string.color_theme_slate
        ColorTheme.GOLD -> R.string.color_theme_gold
        ColorTheme.TEAL -> R.string.color_theme_teal
        ColorTheme.PLUM -> R.string.color_theme_plum
        ColorTheme.MOSS -> R.string.color_theme_moss
        ColorTheme.CORAL -> R.string.color_theme_coral
        ColorTheme.INDIGO -> R.string.color_theme_indigo
        ColorTheme.MUSTARD -> R.string.color_theme_mustard
        ColorTheme.CRIMSON -> R.string.color_theme_crimson
        ColorTheme.MINT -> R.string.color_theme_mint
        ColorTheme.PERIWINKLE -> R.string.color_theme_periwinkle
    },
)

@Composable
fun ThemeContrast.label(): String = stringResource(
    when (this) {
        ThemeContrast.STANDARD -> R.string.contrast_standard
        ThemeContrast.MEDIUM -> R.string.contrast_medium
        ThemeContrast.HIGH -> R.string.contrast_high
    },
)

@Composable
fun MinFileAge.label(): String = stringResource(
    when (this) {
        MinFileAge.ONE_HOUR -> R.string.min_age_hour
        MinFileAge.ONE_DAY -> R.string.min_age_day
        MinFileAge.ONE_WEEK -> R.string.min_age_week
    },
)

@Composable
fun AutoCleanInterval.label(): String = stringResource(
    when (this) {
        AutoCleanInterval.OFF -> R.string.auto_clean_off
        AutoCleanInterval.DAILY -> R.string.auto_clean_daily
        AutoCleanInterval.WEEKLY -> R.string.auto_clean_weekly
    },
)
