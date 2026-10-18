package com.wwwescape.devicehousekeeping.data.settings

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** [seedHue] is a 0-359 HSV hue driving [com.wwwescape.devicehousekeeping.ui.theme.generateColorScheme].
 * DEFAULT (null) keeps the app's own hand-authored static palette (see Color.kt) instead of a
 * generated one. Ignored entirely when dynamic color is on, which takes precedence. */
enum class ColorTheme(val seedHue: Float?) {
    DEFAULT(null),
    OCEAN(205f),
    FOREST(140f),
    SUNSET(25f),
    GRAPE(280f),
    ROSE(340f),
    SLATE(220f),
    GOLD(45f),
    TEAL(175f),
    PLUM(300f),
    MOSS(95f),
    CORAL(12f),
    INDIGO(245f),
    MUSTARD(55f),
    CRIMSON(355f),
    MINT(160f),
    PERIWINKLE(230f),
}

/** Only affects (or only makes sense for) dark theme. */
enum class ThemeContrast { STANDARD, MEDIUM, HIGH }

/** How old a temporary or log file must be before the scanner offers it. Files younger than this
 * may still be in use by the app that wrote them (a download in progress, a log being appended). */
enum class MinFileAge(val millis: Long) {
    ONE_HOUR(60L * 60 * 1000),
    ONE_DAY(24L * 60 * 60 * 1000),
    ONE_WEEK(7L * 24 * 60 * 60 * 1000),
}

/** How often the background job cleans Safe categories on its own. [days] is null for OFF. */
enum class AutoCleanInterval(val days: Long?) {
    OFF(null),
    DAILY(1),
    WEEKLY(7),
}

data class CleaningSettings(
    val minFileAge: MinFileAge = MinFileAge.ONE_DAY,
    /** Scheduled cleaning of Safe categories only — Review items are never cleaned unattended. */
    val autoClean: AutoCleanInterval = AutoCleanInterval.OFF,
    /** Show a summary dialog before anything is deleted. */
    val confirmBeforeCleaning: Boolean = true,
    /** Also scan SD cards and USB drives (Android 11+, where all-files access covers them). */
    val scanRemovableStorage: Boolean = true,
    /** Absolute paths the scanner never offers (and the cleaner never touches), nor anything
     * beneath them. */
    val excludedPaths: Set<String> = emptySet(),
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val colorTheme: ColorTheme = ColorTheme.DEFAULT,
    val themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    val pureDark: Boolean = false,
    val absoluteDark: Boolean = false,
    val cleaning: CleaningSettings = CleaningSettings(),
)
