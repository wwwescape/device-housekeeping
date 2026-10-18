package com.wwwescape.devicehousekeeping.ui.screens.category

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.scan.ItemDetail
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.Safety
import com.wwwescape.devicehousekeeping.ui.CleanerPhase
import com.wwwescape.devicehousekeeping.ui.HousekeepingViewModel
import com.wwwescape.devicehousekeeping.ui.components.AnimatedBytes
import com.wwwescape.devicehousekeeping.ui.components.CategoryBadge
import com.wwwescape.devicehousekeeping.ui.components.EmptyStateNotice
import com.wwwescape.devicehousekeeping.ui.components.MetaPill
import com.wwwescape.devicehousekeeping.ui.components.SafetyChip
import com.wwwescape.devicehousekeeping.ui.components.explanation
import com.wwwescape.devicehousekeeping.ui.components.title
import com.wwwescape.devicehousekeeping.util.formatBytes

private enum class SortOrder { SIZE, NAME, DATE }

@Composable
fun CategoryScreen(
    category: JunkCategory,
    viewModel: HousekeepingViewModel,
    modifier: Modifier = Modifier,
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val result = (phase as? CleanerPhase.Results)?.result
    var sort by rememberSaveable { mutableStateOf(SortOrder.SIZE) }
    var detailsFor by remember { mutableStateOf<JunkItem?>(null) }

    val items = remember(result, category) { result?.itemsIn(category).orEmpty() }
    val sorted = remember(items, sort) {
        when (sort) {
            SortOrder.SIZE -> items.sortedByDescending { it.sizeBytes }
            SortOrder.NAME -> items.sortedBy { it.name.lowercase() }
            SortOrder.DATE -> items.sortedBy { it.lastModified }
        }
    }
    val selectedCount = items.count { it.path in selection }
    val selectedBytes = items.filter { it.path in selection }.sumOf { it.sizeBytes }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hero") {
                CategoryHero(
                    category = category,
                    totalBytes = items.sumOf { it.sizeBytes },
                    selectedBytes = selectedBytes,
                    itemCount = items.size,
                )
            }
            item(key = "why") { ExplanationCard(category) }

            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyStateNotice(
                        title = stringResource(R.string.category_empty_title),
                        body = stringResource(if (result == null) R.string.category_no_results_body else R.string.category_empty_body),
                        icon = Icons.Rounded.SearchOff,
                    )
                }
            } else {
                item(key = "controls") {
                    Controls(
                        state = when (selectedCount) {
                            0 -> ToggleableState.Off
                            items.size -> ToggleableState.On
                            else -> ToggleableState.Indeterminate
                        },
                        selectedCount = selectedCount,
                        total = items.size,
                        sort = sort,
                        onSort = { sort = it },
                        onToggleAll = { viewModel.setSelected(items, selectedCount != items.size) },
                    )
                }
                items(sorted, key = { it.path }) { item ->
                    JunkItemRow(
                        item = item,
                        selected = item.path in selection,
                        onToggle = { viewModel.toggle(item.path) },
                        onDetails = { detailsFor = item },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    detailsFor?.let { item ->
        ItemDetailsSheet(
            item = item,
            onExclude = {
                viewModel.exclude(item)
                detailsFor = null
            },
            onDismiss = { detailsFor = null },
        )
    }
}

@Composable
private fun CategoryHero(category: JunkCategory, totalBytes: Long, selectedBytes: Long, itemCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CategoryBadge(category, size = 56.dp)
            }
            SafetyChip(category.safety, modifier = Modifier.padding(top = 12.dp))
            AnimatedBytes(
                bytes = totalBytes,
                numberStyle = MaterialTheme.typography.displaySmall,
                unitStyle = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp),
            )
            val locale = LocalConfiguration.current.locales[0]
            Text(
                text = stringResource(
                    R.string.category_hero_caption,
                    pluralStringResource(R.plurals.items_count, itemCount, itemCount),
                    formatBytes(selectedBytes, locale),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ExplanationCard(category: JunkCategory) {
    val safe = category.safety == Safety.SAFE
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (safe) Icons.Rounded.VerifiedUser else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (safe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(if (safe) R.string.category_why_safe else R.string.category_why_review),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = category.explanation(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Controls(
    state: ToggleableState,
    selectedCount: Int,
    total: Int,
    sort: SortOrder,
    onSort: (SortOrder) -> Unit,
    onToggleAll: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onToggleAll)) {
            TriStateCheckbox(state = state, onClick = onToggleAll)
            Text(
                text = stringResource(R.string.category_select_all, selectedCount, total),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
            SortOrder.entries.forEach { order ->
                FilterChip(
                    selected = sort == order,
                    onClick = { onSort(order) },
                    label = {
                        Text(
                            stringResource(
                                when (order) {
                                    SortOrder.SIZE -> R.string.sort_size
                                    SortOrder.NAME -> R.string.sort_name
                                    SortOrder.DATE -> R.string.sort_oldest
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

private val JunkItem.typeIcon: ImageVector
    get() = when {
        detail is ItemDetail.Apk || detail == ItemDetail.UnreadableApk -> Icons.Rounded.Android
        isDirectory -> Icons.Rounded.Folder
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }

@Composable
private fun JunkItemRow(
    item: JunkItem,
    selected: Boolean,
    onToggle: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "rowContainer",
    )
    val locale = LocalConfiguration.current.locales[0]
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Icon(
                imageVector = item.typeIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = itemTitle(item),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Text(
                    text = item.path.substringBeforeLast('/').removePrefix(item.volumeRoot).ifEmpty { "/" },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                detailLine(item)?.let { (text, warn) ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (warn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    MetaPill(relativeAge(item.lastModified))
                    if (item.isDirectory) {
                        MetaPill(pluralStringResource(R.plurals.files_count, item.fileCount, item.fileCount))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = formatBytes(item.sizeBytes, locale), style = MaterialTheme.typography.titleSmall)
            }
            IconButton(onClick = onDetails) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_details))
            }
        }
    }
}

/** Residuals and app caches read better by app than by folder name. */
@Composable
internal fun itemTitle(item: JunkItem): String = when (val d = item.detail) {
    is ItemDetail.AppCache -> d.label ?: d.packageName
    is ItemDetail.Apk -> d.label?.let { "$it · ${item.name}" } ?: item.name
    is ItemDetail.Trashed, is ItemDetail.Pending -> item.name.replace(Regex("""^\.(trashed|pending)-\d+-"""), "")
    else -> item.name
}

/** The "why this item" line, and whether it deserves a cautionary tint. */
@Composable
internal fun detailLine(item: JunkItem): Pair<String, Boolean>? = when (val d = item.detail) {
    is ItemDetail.Apk -> {
        val apkVersion = d.apkVersionName ?: d.apkVersionCode.toString()
        if (d.installedVersionCode == null) {
            stringResource(R.string.detail_apk_not_installed, apkVersion) to false
        } else {
            stringResource(R.string.detail_apk_installed, apkVersion, d.installedVersionName ?: d.installedVersionCode.toString()) to
                (d.installedVersionCode < d.apkVersionCode)
        }
    }
    ItemDetail.UnreadableApk -> stringResource(R.string.detail_apk_unreadable) to false
    is ItemDetail.Residual -> {
        val base = stringResource(R.string.detail_residual, d.packageName)
        if (d.mediaFiles > 0) {
            (base + " · " + pluralStringResource(R.plurals.detail_residual_media, d.mediaFiles, d.mediaFiles)) to true
        } else {
            base to false
        }
    }
    is ItemDetail.AppCache -> stringResource(R.string.detail_app_cache, d.packageName) to false
    is ItemDetail.Trashed -> d.expiresAt?.let {
        stringResource(R.string.detail_trashed_expires, relativeAge(it)) to false
    } ?: (stringResource(R.string.detail_trashed) to false)
    is ItemDetail.Pending -> stringResource(R.string.detail_pending) to false
    null -> null
}

@Composable
internal fun relativeAge(time: Long): String {
    val now = remember { System.currentTimeMillis() }
    return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString()
}
