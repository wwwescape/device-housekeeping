package com.wwwescape.devicehousekeeping.ui.screens.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.clean.CleanProgress
import com.wwwescape.devicehousekeeping.data.clean.CleanReport
import com.wwwescape.devicehousekeeping.data.history.History
import com.wwwescape.devicehousekeeping.data.history.headlineBytes
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.Safety
import com.wwwescape.devicehousekeeping.data.scan.ScanProgress
import com.wwwescape.devicehousekeeping.data.scan.ScanResult
import com.wwwescape.devicehousekeeping.ui.AppCacheState
import com.wwwescape.devicehousekeeping.ui.components.AnimatedBytes
import com.wwwescape.devicehousekeeping.ui.components.CategoryBadge
import com.wwwescape.devicehousekeeping.ui.components.CleaningRing
import com.wwwescape.devicehousekeeping.ui.components.DoneBurst
import com.wwwescape.devicehousekeeping.ui.components.EmptyStateNotice
import com.wwwescape.devicehousekeeping.ui.components.MetaPill
import com.wwwescape.devicehousekeeping.ui.components.RowDivider
import com.wwwescape.devicehousekeeping.ui.components.RowIcon
import com.wwwescape.devicehousekeeping.ui.components.SafetyChip
import com.wwwescape.devicehousekeeping.ui.components.ScanOrb
import com.wwwescape.devicehousekeeping.ui.components.ScanRadar
import com.wwwescape.devicehousekeeping.ui.components.SectionLabel
import com.wwwescape.devicehousekeeping.ui.components.SettingsGroupCard
import com.wwwescape.devicehousekeeping.ui.components.StaggeredEntrance
import com.wwwescape.devicehousekeeping.ui.components.title
import com.wwwescape.devicehousekeeping.ui.components.summary
import com.wwwescape.devicehousekeeping.ui.theme.PillShape
import com.wwwescape.devicehousekeeping.util.formatBytes
import java.text.NumberFormat
import java.util.Locale

@Composable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun formatCount(count: Int): String = remember(count) { NumberFormat.getIntegerInstance().format(count) }

// region Idle

@Composable
internal fun IdleStage(history: History, onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScanOrb(label = stringResource(R.string.action_scan), onClick = onScan)
        Text(
            text = stringResource(R.string.idle_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.idle_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(top = 6.dp, start = 8.dp, end = 8.dp),
        )
        history.entries.firstOrNull()?.let { last ->
            val now = remember { System.currentTimeMillis() }
            MetaPill(
                text = stringResource(
                    R.string.idle_last_cleaned,
                    DateUtils.getRelativeTimeSpanString(last.timestamp, now, DateUtils.MINUTE_IN_MILLIS).toString(),
                    formatBytes(last.headlineBytes, locale()),
                ),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

// endregion

// region Scanning

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScanningStage(progress: ScanProgress, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScanRadar(foundCategories = progress.foundByCategory.keys) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedBytes(
                    bytes = progress.foundBytes,
                    numberStyle = MaterialTheme.typography.headlineMedium,
                    unitStyle = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    durationMillis = 300,
                )
                Text(
                    text = stringResource(R.string.scan_found),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = stringResource(R.string.scan_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = pluralStringResource(R.plurals.scan_files_checked, progress.filesScanned, formatCount(progress.filesScanned)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = progress.currentPath,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            JunkCategory.entries.forEach { category ->
                val bytes = progress.foundByCategory[category]
                AnimatedVisibility(visible = bytes != null, enter = fadeIn() + scaleIn(initialScale = 0.6f)) {
                    Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryBadge(category, size = 24.dp)
                            Text(
                                text = "${category.title()} · ${formatBytes(bytes ?: 0, locale())}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onCancel, shape = PillShape) { Text(stringResource(R.string.action_cancel)) }
    }
}

// endregion

// region Results

@Composable
internal fun ResultsStage(
    result: ScanResult,
    selection: Set<String>,
    onToggleCategory: (JunkCategory, Boolean) -> Unit,
    onOpenCategory: (JunkCategory) -> Unit,
    onOpenAppCaches: () -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultSummaryCard(result = result, onRescan = onRescan)

        if (result.items.isEmpty()) {
            EmptyStateNotice(
                title = stringResource(R.string.results_empty_title),
                body = stringResource(R.string.results_empty_body),
                icon = Icons.Rounded.AutoAwesome,
            )
            Button(onClick = onDismiss, shape = PillShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.action_done))
            }
        }

        val safe = result.categories.filter { it.safety == Safety.SAFE }
        val review = result.categories.filter { it.safety == Safety.REVIEW }
        var index = 0
        if (safe.isNotEmpty()) {
            SectionLabel(stringResource(R.string.section_safe_to_clean))
            safe.forEach { category ->
                StaggeredEntrance(index = index++, key = "${result.finishedAt}-$category") {
                    CategoryCard(category, result, selection, onToggleCategory, onOpenCategory)
                }
            }
        }
        if (review.isNotEmpty()) {
            Spacer8()
            SectionLabel(stringResource(R.string.section_review))
            Text(
                text = stringResource(R.string.section_review_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            review.forEach { category ->
                StaggeredEntrance(index = index++, key = "${result.finishedAt}-$category") {
                    CategoryCard(category, result, selection, onToggleCategory, onOpenCategory)
                }
            }
        }

        if (!result.appCachesReachable) {
            InfoRow(
                icon = Icons.Rounded.Apps,
                title = stringResource(R.string.results_app_caches_title),
                body = stringResource(R.string.results_app_caches_body),
                onClick = onOpenAppCaches,
            )
        }
        if (!result.residualsChecked) {
            InfoRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.results_residuals_skipped_title),
                body = stringResource(R.string.results_residuals_skipped_body),
            )
        }
    }
}

@Composable
private fun Spacer8() = androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))

@Composable
private fun ResultSummaryCard(result: ScanResult, onRescan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.results_found_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AnimatedBytes(
                    bytes = result.totalBytes,
                    numberStyle = MaterialTheme.typography.displaySmall,
                    unitStyle = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    durationMillis = 1200,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        R.string.results_stats,
                        pluralStringResource(R.plurals.items_count, result.items.size, formatCount(result.items.size)),
                        pluralStringResource(R.plurals.scan_files_checked, result.filesScanned, formatCount(result.filesScanned)),
                        String.format(locale(), "%.1f", result.durationMillis / 1000f),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onRescan) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_rescan))
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: JunkCategory,
    result: ScanResult,
    selection: Set<String>,
    onToggle: (JunkCategory, Boolean) -> Unit,
    onOpen: (JunkCategory) -> Unit,
) {
    val items = remember(result, category) { result.itemsIn(category) }
    val total = remember(items) { items.sumOf { it.sizeBytes } }
    val selectedItems = items.count { it.path in selection }
    val selectedBytes = items.filter { it.path in selection }.sumOf { it.sizeBytes }
    val state = when (selectedItems) {
        0 -> ToggleableState.Off
        items.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val container by animateColorAsState(
        if (selectedItems > 0) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "categoryContainer",
    )
    val share by animateFloatAsState(
        targetValue = if (result.totalBytes > 0) total.toFloat() / result.totalBytes else 0f,
        animationSpec = tween(900),
        label = "share",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(category) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(category)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(text = category.title(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = category.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        SafetyChip(category.safety)
                        MetaPill(
                            text = if (selectedItems in 1 until items.size) {
                                stringResource(R.string.category_selected_of, selectedItems, items.size)
                            } else {
                                pluralStringResource(R.plurals.items_count, items.size, formatCount(items.size))
                            },
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = formatBytes(if (selectedItems in 1 until items.size) selectedBytes else total, locale()),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selectedItems > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TriStateCheckbox(state = state, onClick = { onToggle(category, state != ToggleableState.On) })
                    }
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            LinearProgressIndicator(
                progress = { share },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 12.dp)
                    .height(4.dp),
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, body: String, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun CleanBar(selectedBytes: Long, selectedCount: Int, onClean: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedBytes(
                    bytes = selectedBytes,
                    numberStyle = MaterialTheme.typography.titleLarge,
                    unitStyle = MaterialTheme.typography.labelLarge,
                    durationMillis = 400,
                    startFromZero = false,
                )
                Text(
                    text = pluralStringResource(R.plurals.items_selected, selectedCount, formatCount(selectedCount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onClean,
                enabled = selectedCount > 0,
                shape = PillShape,
                modifier = Modifier.height(56.dp),
            ) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                Text(stringResource(R.string.action_clean), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
internal fun ConfirmCleanDialog(items: List<JunkItem>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val total = items.sumOf { it.sizeBytes }
    val byCategory = remember(items) { items.groupBy { it.category }.toSortedMap(compareBy { it.ordinal }) }
    val reviewCount = items.count { it.category.safety == Safety.REVIEW }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.CleaningServices, contentDescription = null) },
        title = { Text(stringResource(R.string.confirm_title, formatBytes(total, locale()))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                byCategory.forEach { (category, list) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(category, size = 28.dp)
                        Text(
                            text = category.title(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                        )
                        Text(
                            text = formatBytes(list.sumOf { it.sizeBytes }, locale()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (reviewCount > 0) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = pluralStringResource(R.plurals.confirm_review_warning, reviewCount, reviewCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.confirm_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm, shape = PillShape) { Text(stringResource(R.string.action_clean)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// endregion

// region Cleaning & done

@Composable
internal fun CleaningStage(progress: CleanProgress) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CleaningRing(progress = progress.fraction) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedBytes(
                    bytes = progress.freedBytes,
                    numberStyle = MaterialTheme.typography.headlineMedium,
                    unitStyle = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    durationMillis = 250,
                )
                Text(
                    text = stringResource(R.string.clean_freed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = stringResource(R.string.clean_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.clean_item_of, progress.itemsDone, progress.itemsTotal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = progress.currentPath,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
internal fun DoneStage(report: CleanReport, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DoneBurst()
        Text(text = stringResource(R.string.done_title), style = MaterialTheme.typography.titleLarge)
        AnimatedBytes(
            bytes = report.freedBytes,
            numberStyle = MaterialTheme.typography.displayMedium,
            unitStyle = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            durationMillis = 1400,
        )
        Text(
            text = stringResource(R.string.done_freed_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsGroupCard(modifier = Modifier.padding(top = 8.dp)) {
            DoneFactRow(
                icon = Icons.AutoMirrored.Rounded.FactCheck,
                label = stringResource(R.string.done_measured),
                value = "+" + formatBytes(report.measuredFreedBytes, locale()),
            )
            RowDivider()
            DoneFactRow(
                icon = Icons.Rounded.CleaningServices,
                label = stringResource(R.string.done_items),
                value = pluralStringResource(R.plurals.done_items_value, report.deletedItems, formatCount(report.deletedItems), formatCount(report.deletedFiles)),
            )
            val skipped = report.skipped.values.sum()
            if (skipped > 0) {
                RowDivider()
                DoneFactRow(
                    icon = Icons.Rounded.Shield,
                    label = stringResource(R.string.done_skipped),
                    value = formatCount(skipped),
                )
                Text(
                    text = stringResource(R.string.done_skipped_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
                )
            }
            if (report.failedFiles > 0) {
                RowDivider()
                DoneFactRow(
                    icon = Icons.Rounded.Lock,
                    label = stringResource(R.string.done_failed),
                    value = formatCount(report.failedFiles),
                )
            }
        }

        if (report.freedByCategory.isNotEmpty()) {
            SettingsGroupCard {
                val max = report.freedByCategory.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                report.freedByCategory.entries.sortedByDescending { it.value }.forEachIndexed { index, (category, bytes) ->
                    if (index > 0) androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                    val fraction by animateFloatAsState(bytes.toFloat() / max, tween(1000, delayMillis = 300 + index * 80), label = "bar")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(category, size = 32.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Row {
                                Text(category.title(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(formatBytes(bytes, locale()), style = MaterialTheme.typography.labelLarge)
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .height(6.dp),
                                strokeCap = StrokeCap.Round,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onFinish,
            shape = PillShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(56.dp),
        ) { Text(stringResource(R.string.action_done)) }
    }
}

@Composable
private fun DoneFactRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

// endregion

// region Idle extras

@Composable
internal fun AppCachesCard(state: AppCacheState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(Icons.Rounded.Apps)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(stringResource(R.string.title_app_caches), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (state) {
                        AppCacheState.Loading -> stringResource(R.string.app_caches_card_loading)
                        AppCacheState.NoAccess -> stringResource(R.string.app_caches_card_no_access)
                        is AppCacheState.Loaded -> pluralStringResource(
                            R.plurals.app_caches_card_summary,
                            state.summary.apps.size,
                            formatBytes(state.summary.totalCacheBytes, locale()),
                            formatCount(state.summary.apps.size),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SafetyPromiseCard() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.section_how_it_stays_safe))
        SettingsGroupCard {
            PromiseRow(Icons.Rounded.Shield, R.string.promise_rules_title, R.string.promise_rules_body)
            RowDivider()
            PromiseRow(Icons.AutoMirrored.Rounded.FactCheck, R.string.promise_recheck_title, R.string.promise_recheck_body)
            RowDivider()
            PromiseRow(Icons.Rounded.Lock, R.string.promise_personal_title, R.string.promise_personal_body)
        }
    }
}

@Composable
private fun PromiseRow(icon: ImageVector, titleRes: Int, bodyRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RowIcon(icon)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion
