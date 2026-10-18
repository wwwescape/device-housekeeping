package com.wwwescape.devicehousekeeping.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.schedule.AutoCleanNotifier
import com.wwwescape.devicehousekeeping.data.settings.AutoCleanInterval
import com.wwwescape.devicehousekeeping.data.settings.MinFileAge
import com.wwwescape.devicehousekeeping.data.settings.ThemeContrast
import com.wwwescape.devicehousekeeping.data.settings.ThemeMode
import com.wwwescape.devicehousekeeping.data.storage.StorageAccess
import com.wwwescape.devicehousekeeping.ui.components.NavigationRow
import com.wwwescape.devicehousekeeping.ui.components.ReadableWidth
import com.wwwescape.devicehousekeeping.ui.components.RowDivider
import com.wwwescape.devicehousekeeping.ui.components.SectionLabel
import com.wwwescape.devicehousekeeping.ui.components.SettingsGroupCard
import com.wwwescape.devicehousekeeping.ui.components.ToggleRow
import com.wwwescape.devicehousekeeping.util.openUrl

private const val PRIVACY_POLICY_URL = "https://www.ericppereira.co.in/apps/device-housekeeping/privacy-policy.html"

private enum class SettingsDialog { THEME, COLOR_THEME, CONTRAST, MIN_AGE, AUTO_CLEAN, FAQ }

@Composable
fun SettingsScreen(
    onNavigateToLicenses: () -> Unit,
    onNavigateToExclusions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var storageAccess by remember { mutableStateOf(StorageAccess.hasStorageAccess(context)) }
    var usageAccess by remember { mutableStateOf(StorageAccess.hasUsageAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        storageAccess = StorageAccess.hasStorageAccess(context)
        usageAccess = StorageAccess.hasUsageAccess(context)
    }
    val cleaning = settings.cleaning
    // The result doesn't gate the feature: automatic cleaning works without notifications, it
    // just runs silently.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val onAutoCleanSelected: (AutoCleanInterval) -> Unit = { interval ->
        viewModel.setAutoClean(interval)
        if (interval != AutoCleanInterval.OFF && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AutoCleanNotifier.canNotify(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ReadableWidth(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel(stringResource(R.string.section_cleaning))
            SettingsGroupCard {
                NavigationRow(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.setting_min_age),
                    subtitle = cleaning.minFileAge.label(),
                    onClick = { dialog = SettingsDialog.MIN_AGE },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.EventRepeat,
                    title = stringResource(R.string.setting_auto_clean),
                    subtitle = if (cleaning.autoClean == AutoCleanInterval.OFF) {
                        stringResource(R.string.setting_auto_clean_subtitle_off)
                    } else {
                        stringResource(R.string.setting_auto_clean_subtitle_on, cleaning.autoClean.label())
                    },
                    onClick = { dialog = SettingsDialog.AUTO_CLEAN },
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.setting_confirm),
                    subtitle = stringResource(R.string.setting_confirm_subtitle),
                    checked = cleaning.confirmBeforeCleaning,
                    onCheckedChange = viewModel::setConfirmBeforeCleaning,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    RowDivider()
                    ToggleRow(
                        icon = Icons.Rounded.SdCard,
                        title = stringResource(R.string.setting_removable),
                        subtitle = stringResource(R.string.setting_removable_subtitle),
                        checked = cleaning.scanRemovableStorage,
                        onCheckedChange = viewModel::setScanRemovableStorage,
                    )
                }
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Block,
                    title = stringResource(R.string.title_exclusions),
                    subtitle = if (cleaning.excludedPaths.isEmpty()) {
                        stringResource(R.string.setting_exclusions_none)
                    } else {
                        stringResource(R.string.setting_exclusions_count, cleaning.excludedPaths.size)
                    },
                    onClick = onNavigateToExclusions,
                )
            }

            SectionLabel(stringResource(R.string.section_access))
            SettingsGroupCard {
                NavigationRow(
                    icon = Icons.Rounded.FolderOpen,
                    title = stringResource(
                        if (StorageAccess.usesAllFilesAccess) R.string.setting_access_all_files else R.string.setting_access_storage,
                    ),
                    subtitle = stringResource(if (storageAccess) R.string.access_granted else R.string.access_required),
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        if (StorageAccess.usesAllFilesAccess) {
                            StorageAccess.launchFirst(context, StorageAccess.allFilesAccessIntents(context))
                        } else {
                            context.startActivity(StorageAccess.appDetailsIntent(context.packageName))
                        }
                    },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.setting_access_usage),
                    subtitle = stringResource(if (usageAccess) R.string.access_granted else R.string.access_optional),
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { StorageAccess.launchFirst(context, StorageAccess.usageAccessIntents(context)) },
                )
            }

            SectionLabel(stringResource(R.string.section_appearance))
            SettingsGroupCard {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ToggleRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.setting_dynamic_color),
                        subtitle = stringResource(R.string.setting_dynamic_color_subtitle),
                        checked = settings.useDynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                    RowDivider()
                }
                NavigationRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.setting_color_theme),
                    subtitle = settings.colorTheme.label(),
                    onClick = { dialog = SettingsDialog.COLOR_THEME },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.DarkMode,
                    title = stringResource(R.string.setting_theme),
                    subtitle = settings.themeMode.label(),
                    onClick = { dialog = SettingsDialog.THEME },
                )
                // Contrast/pure dark/absolute dark only affect dark theme — hidden entirely when the
                // effective theme (accounting for "System default" resolving to light) is light.
                if (isDarkTheme) {
                    RowDivider()
                    NavigationRow(
                        icon = Icons.Rounded.Contrast,
                        title = stringResource(R.string.setting_theme_contrast),
                        subtitle = settings.themeContrast.label(),
                        onClick = { dialog = SettingsDialog.CONTRAST },
                    )
                    RowDivider()
                    ToggleRow(
                        icon = Icons.Rounded.Brightness6,
                        title = stringResource(R.string.setting_pure_dark),
                        subtitle = stringResource(R.string.setting_pure_dark_subtitle),
                        checked = settings.pureDark,
                        onCheckedChange = viewModel::setPureDark,
                    )
                    RowDivider()
                    ToggleRow(
                        icon = Icons.Rounded.NightsStay,
                        title = stringResource(R.string.setting_absolute_dark),
                        subtitle = stringResource(R.string.setting_absolute_dark_subtitle),
                        checked = settings.absoluteDark,
                        onCheckedChange = viewModel::setAbsoluteDark,
                    )
                }
            }

            SectionLabel(stringResource(R.string.section_about))
            SettingsGroupCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_app_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RowDivider()
                Text(
                    text = stringResource(R.string.privacy_statement_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.QuestionAnswer,
                    title = stringResource(R.string.setting_faq),
                    subtitle = stringResource(R.string.setting_faq_subtitle),
                    onClick = { dialog = SettingsDialog.FAQ },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.section_privacy_policy),
                    subtitle = null,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { openUrl(context, PRIVACY_POLICY_URL) },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.section_open_source_licenses),
                    subtitle = stringResource(R.string.settings_row_licenses_subtitle),
                    onClick = onNavigateToLicenses,
                )
            }
        }
    }

    val dismiss = { dialog = null }
    when (dialog) {
        SettingsDialog.THEME -> SettingsPickerDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label() },
            onSelect = viewModel::setThemeMode,
            onDismiss = dismiss,
        )
        SettingsDialog.COLOR_THEME -> ThemePickerDialog(
            selected = settings.colorTheme,
            onSelect = viewModel::setColorTheme,
            onDismiss = dismiss,
        )
        SettingsDialog.CONTRAST -> SettingsPickerDialog(
            title = stringResource(R.string.setting_theme_contrast),
            options = ThemeContrast.entries,
            selected = settings.themeContrast,
            label = { it.label() },
            onSelect = viewModel::setThemeContrast,
            onDismiss = dismiss,
        )
        SettingsDialog.MIN_AGE -> SettingsPickerDialog(
            title = stringResource(R.string.setting_min_age),
            options = MinFileAge.entries,
            selected = cleaning.minFileAge,
            label = { it.label() },
            onSelect = viewModel::setMinFileAge,
            onDismiss = dismiss,
        )
        SettingsDialog.AUTO_CLEAN -> SettingsPickerDialog(
            title = stringResource(R.string.setting_auto_clean),
            options = AutoCleanInterval.entries,
            selected = cleaning.autoClean,
            label = { it.label() },
            onSelect = onAutoCleanSelected,
            onDismiss = dismiss,
        )
        SettingsDialog.FAQ -> FaqDialog(onDismiss = dismiss)
        null -> Unit
    }
}
