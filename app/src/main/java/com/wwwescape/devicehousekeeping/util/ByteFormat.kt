package com.wwwescape.devicehousekeeping.util

import java.util.Locale

private val UNITS = listOf("B", "KB", "MB", "GB", "TB")

/** A byte count split into its number and unit, so the UI can style them separately. */
data class ByteParts(val number: String, val unit: String) {
    override fun toString() = "$number $unit"
}

/** Formats a byte count using decimal (1000-based) units, matching Android's own storage UI.
 * Values under 10 get two decimals, under 100 one, and larger ones none — so the string keeps a
 * steady width while a count-up animation runs through it. */
fun byteParts(bytes: Long, locale: Locale = Locale.getDefault()): ByteParts {
    if (bytes <= 0) return ByteParts("0", UNITS[0])
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1000 && unitIndex < UNITS.lastIndex) {
        value /= 1000
        unitIndex++
    }
    val number = when {
        unitIndex == 0 -> value.toLong().toString()
        value < 10 -> String.format(locale, "%.2f", value)
        value < 100 -> String.format(locale, "%.1f", value)
        else -> String.format(locale, "%.0f", value)
    }
    return ByteParts(number, UNITS[unitIndex])
}

fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String = byteParts(bytes, locale).toString()
