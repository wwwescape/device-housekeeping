package com.wwwescape.devicehousekeeping.ui.screens.history

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.history.HistoryEntry
import com.wwwescape.devicehousekeeping.data.history.HistoryKind
import com.wwwescape.devicehousekeeping.data.history.headlineBytes
import com.wwwescape.devicehousekeeping.ui.HousekeepingViewModel
import com.wwwescape.devicehousekeeping.ui.components.AnimatedBytes
import com.wwwescape.devicehousekeeping.ui.components.CategoryBadge
import com.wwwescape.devicehousekeeping.ui.components.EmptyStateNotice
import com.wwwescape.devicehousekeeping.ui.components.MetaPill
import com.wwwescape.devicehousekeeping.ui.components.RowIcon
import com.wwwescape.devicehousekeeping.ui.components.StaggeredEntrance
import com.wwwescape.devicehousekeeping.ui.components.title
import com.wwwescape.devicehousekeeping.util.formatBytes
import java.util.Date

@Composable
fun HistoryScreen(viewModel: HousekeepingViewModel, modifier: Modifier = Modifier) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hero") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.history_lifetime_label).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        AnimatedBytes(
                            bytes = history.lifetimeFreedBytes,
                            numberStyle = MaterialTheme.typography.displayMedium,
                            unitStyle = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            durationMillis = 1400,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            pluralStringResource(R.plurals.history_runs, history.lifetimeRuns.toInt(), history.lifetimeRuns.toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
            if (history.entries.isEmpty()) {
                item(key = "empty") {
                    EmptyStateNotice(
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                        icon = Icons.Rounded.History,
                    )
                }
            } else {
                item(key = "header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.history_recent).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                        )
                        TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.action_clear_history)) }
                    }
                }
                itemsIndexed(history.entries, key = { _, e -> "${e.timestamp}-${e.kind}" }) { index, entry ->
                    StaggeredEntrance(index = index.coerceAtMost(10), key = entry.timestamp, staggerMillis = 40) {
                        HistoryRow(entry)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirmClear = false
                }) { Text(stringResource(R.string.action_clear_history)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val date = Date(entry.timestamp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(
                    when (entry.kind) {
                        HistoryKind.CLEAN -> Icons.Rounded.CleaningServices
                        HistoryKind.AUTO_CLEAN -> Icons.Rounded.EventRepeat
                        HistoryKind.SYSTEM_CACHE -> Icons.Rounded.AutoDelete
                    },
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        stringResource(
                            when (entry.kind) {
                                HistoryKind.CLEAN -> R.string.history_kind_clean
                                HistoryKind.AUTO_CLEAN -> R.string.history_kind_auto
                                HistoryKind.SYSTEM_CACHE -> R.string.history_kind_system
                            },
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        DateFormat.getMediumDateFormat(context).format(date) + " · " + DateFormat.getTimeFormat(context).format(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formatBytes(entry.headlineBytes, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp, start = 48.dp)) {
                if (entry.kind != HistoryKind.SYSTEM_CACHE) {
                    MetaPill(pluralStringResource(R.plurals.items_count, entry.itemCount, entry.itemCount))
                }
                MetaPill(stringResource(R.string.history_measured, formatBytes(entry.measuredFreedBytes, locale)))
            }
            if (entry.byCategory.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 10.dp, start = 48.dp),
                ) {
                    entry.byCategory.entries.sortedByDescending { it.value }.forEach { (category, bytes) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(category, size = 20.dp)
                            Text(
                                "${category.title()} ${formatBytes(bytes, locale)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
