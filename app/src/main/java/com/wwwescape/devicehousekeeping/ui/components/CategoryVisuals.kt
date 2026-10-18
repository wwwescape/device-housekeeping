package com.wwwescape.devicehousekeeping.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.Safety

val JunkCategory.icon: ImageVector
    get() = when (this) {
        JunkCategory.APP_CACHES -> Icons.Rounded.Cached
        JunkCategory.THUMBNAILS -> Icons.Rounded.PhotoLibrary
        JunkCategory.TEMP_FILES -> Icons.Rounded.HourglassEmpty
        JunkCategory.LOGS -> Icons.Rounded.BugReport
        JunkCategory.FOREIGN_FILES -> Icons.Rounded.Computer
        JunkCategory.INSTALLED_APKS -> Icons.Rounded.Android
        JunkCategory.OTHER_APKS -> Icons.Rounded.InstallMobile
        JunkCategory.RESIDUALS -> Icons.Rounded.FolderDelete
        JunkCategory.TRASH -> Icons.Rounded.DeleteSweep
    }

@Composable
fun JunkCategory.title(): String = stringResource(
    when (this) {
        JunkCategory.APP_CACHES -> R.string.category_app_caches
        JunkCategory.THUMBNAILS -> R.string.category_thumbnails
        JunkCategory.TEMP_FILES -> R.string.category_temp_files
        JunkCategory.LOGS -> R.string.category_logs
        JunkCategory.FOREIGN_FILES -> R.string.category_foreign_files
        JunkCategory.INSTALLED_APKS -> R.string.category_installed_apks
        JunkCategory.OTHER_APKS -> R.string.category_other_apks
        JunkCategory.RESIDUALS -> R.string.category_residuals
        JunkCategory.TRASH -> R.string.category_trash
    },
)

/** One line, shown under the title on Home. */
@Composable
fun JunkCategory.summary(): String = stringResource(
    when (this) {
        JunkCategory.APP_CACHES -> R.string.category_app_caches_summary
        JunkCategory.THUMBNAILS -> R.string.category_thumbnails_summary
        JunkCategory.TEMP_FILES -> R.string.category_temp_files_summary
        JunkCategory.LOGS -> R.string.category_logs_summary
        JunkCategory.FOREIGN_FILES -> R.string.category_foreign_files_summary
        JunkCategory.INSTALLED_APKS -> R.string.category_installed_apks_summary
        JunkCategory.OTHER_APKS -> R.string.category_other_apks_summary
        JunkCategory.RESIDUALS -> R.string.category_residuals_summary
        JunkCategory.TRASH -> R.string.category_trash_summary
    },
)

/** The full "why is this safe / what should I check" explanation on the category screen. */
@Composable
fun JunkCategory.explanation(): String = stringResource(
    when (this) {
        JunkCategory.APP_CACHES -> R.string.category_app_caches_why
        JunkCategory.THUMBNAILS -> R.string.category_thumbnails_why
        JunkCategory.TEMP_FILES -> R.string.category_temp_files_why
        JunkCategory.LOGS -> R.string.category_logs_why
        JunkCategory.FOREIGN_FILES -> R.string.category_foreign_files_why
        JunkCategory.INSTALLED_APKS -> R.string.category_installed_apks_why
        JunkCategory.OTHER_APKS -> R.string.category_other_apks_why
        JunkCategory.RESIDUALS -> R.string.category_residuals_why
        JunkCategory.TRASH -> R.string.category_trash_why
    },
)

/** Container/content colors for the category's icon badge. Safe categories alternate between
 * the primary and secondary tones; review categories all use tertiary, so they read as a group. */
@Composable
fun JunkCategory.badgeColors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when {
        safety == Safety.REVIEW -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        ordinal % 2 == 0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
}

@Composable
fun Safety.label(): String = stringResource(
    when (this) {
        Safety.SAFE -> R.string.safety_safe
        Safety.REVIEW -> R.string.safety_review
    },
)
