package com.wwwescape.devicehousekeeping.ui.screens.apps

import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.apps.AppCacheEntry
import com.wwwescape.devicehousekeeping.data.storage.StorageAccess
import com.wwwescape.devicehousekeeping.ui.AppCacheState
import com.wwwescape.devicehousekeeping.ui.HousekeepingViewModel
import com.wwwescape.devicehousekeeping.ui.components.AnimatedBytes
import com.wwwescape.devicehousekeeping.ui.components.PermissionCard
import com.wwwescape.devicehousekeeping.ui.components.RowIcon
import com.wwwescape.devicehousekeeping.ui.components.SectionLabel
import com.wwwescape.devicehousekeeping.ui.components.StaggeredEntrance
import com.wwwescape.devicehousekeeping.ui.theme.PillShape
import com.wwwescape.devicehousekeeping.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppCachesScreen(viewModel: HousekeepingViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by viewModel.appCaches.collectAsStateWithLifecycle()
    val freedBySystem by viewModel.systemCacheFreed.collectAsStateWithLifecycle()
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var hasStorageAccess by remember { mutableStateOf(StorageAccess.hasStorageAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasStorageAccess = StorageAccess.hasStorageAccess(context)
        viewModel.loadAppCaches()
    }

    var freeBefore by remember { mutableLongStateOf(0L) }
    /** Why the last tap on "Clear app caches" didn't clear anything, if it didn't. */
    var clearNotice by remember { mutableStateOf<Int?>(null) }
    val clearLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onSystemCacheCleared(freeBefore)
        } else {
            clearNotice = R.string.app_caches_clear_cancelled
        }
    }
    // The dialog lives in MediaProvider; some OEM builds (seen on a OnePlus tablet) leave it out
    // or answer "cancelled" without showing anything. Check up front rather than fail silently.
    val clearDialogAvailable = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            context.packageManager.resolveActivity(Intent(StorageManager.ACTION_CLEAR_APP_CACHE), 0) != null
    }
    val canClearExternal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasStorageAccess && clearDialogAvailable
    val openSystemStorage = {
        StorageAccess.launchFirst(
            context,
            listOf(
                Intent(StorageManager.ACTION_MANAGE_STORAGE),
                Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hero") { AppCachesHero(state) }

            freedBySystem?.let { freed ->
                item(key = "freed") {
                    LaunchedEffect(freed) {
                        kotlinx.coroutines.delay(6000)
                        viewModel.consumeSystemCacheFreed()
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Text(
                            text = stringResource(R.string.app_caches_system_freed, formatBytes(freed)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                item(key = "clearExternal") {
                    ActionCard(
                        icon = Icons.Rounded.AutoDelete,
                        title = stringResource(R.string.app_caches_clear_external_title),
                        body = stringResource(
                            when {
                                !clearDialogAvailable -> R.string.app_caches_clear_external_unsupported
                                !hasStorageAccess -> R.string.app_caches_clear_external_needs_access
                                else -> R.string.app_caches_clear_external_body
                            },
                        ),
                        // Without the system dialog, the button leads to Android's own storage
                        // tools instead of doing nothing.
                        action = stringResource(if (clearDialogAvailable) R.string.action_clear_app_caches else R.string.action_open_storage_settings),
                        enabled = canClearExternal || !clearDialogAvailable,
                        primary = true,
                        onClick = {
                            clearNotice = null
                            if (!clearDialogAvailable) {
                                openSystemStorage()
                            } else {
                                freeBefore = viewModel.sampleFreeBytes()
                                val launched = runCatching { clearLauncher.launch(Intent(StorageManager.ACTION_CLEAR_APP_CACHE)) }.isSuccess
                                if (!launched) clearNotice = R.string.app_caches_clear_unavailable
                            }
                        },
                    )
                }
            }
            clearNotice?.let { notice ->
                item(key = "clearNotice") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(
                            text = stringResource(notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item(key = "systemStorage") {
                ActionCard(
                    icon = Icons.Rounded.Storage,
                    title = stringResource(R.string.app_caches_system_tool_title),
                    body = stringResource(R.string.app_caches_system_tool_body),
                    action = stringResource(R.string.action_open),
                    enabled = true,
                    primary = false,
                    onClick = openSystemStorage,
                )
            }

            when (val s = state) {
                AppCacheState.NoAccess -> item(key = "noAccess") {
                    PermissionCard(
                        icon = Icons.Rounded.Insights,
                        title = stringResource(R.string.permission_usage_title),
                        body = stringResource(R.string.permission_usage_body),
                        action = stringResource(R.string.action_grant),
                        onClick = { StorageAccess.launchFirst(context, StorageAccess.usageAccessIntents(context)) },
                    )
                }
                AppCacheState.Loading -> item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AppCacheState.Loaded -> {
                    val apps = s.summary.apps.filter { showSystem || !it.isSystem }
                    val max = apps.maxOfOrNull { it.cacheBytes }?.coerceAtLeast(1) ?: 1
                    item(key = "listHeader") {
                        Column {
                            SectionLabel(stringResource(R.string.app_caches_list_title))
                            Text(
                                text = stringResource(R.string.app_caches_list_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                            )
                            FilterChip(
                                selected = showSystem,
                                onClick = { showSystem = !showSystem },
                                label = { Text(stringResource(R.string.app_caches_show_system)) },
                            )
                        }
                    }
                    items(apps, key = { it.packageName }) { app ->
                        val index = apps.indexOf(app)
                        StaggeredEntrance(index = index.coerceAtMost(12), key = app.packageName, staggerMillis = 35) {
                            AppRow(app = app, max = max, onClick = { runCatching { context.startActivity(StorageAccess.appDetailsIntent(app.packageName)) } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCachesHero(state: AppCacheState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.app_caches_hero_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            val summary = (state as? AppCacheState.Loaded)?.summary
            AnimatedBytes(
                bytes = summary?.totalCacheBytes ?: 0,
                numberStyle = MaterialTheme.typography.displaySmall,
                unitStyle = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = when {
                    summary == null -> stringResource(R.string.app_caches_hero_unknown)
                    summary.totalExternalCacheBytes != null -> pluralStringResource(
                        R.plurals.app_caches_hero_detail_external,
                        summary.apps.size,
                        summary.apps.size,
                        formatBytes(summary.totalExternalCacheBytes),
                    )
                    else -> pluralStringResource(R.plurals.app_caches_hero_detail, summary.apps.size, summary.apps.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text = stringResource(R.string.app_caches_hero_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(icon)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 48.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                if (primary) {
                    Button(onClick = onClick, enabled = enabled, shape = PillShape) { Text(action) }
                } else {
                    OutlinedButton(onClick = onClick, enabled = enabled, shape = PillShape) { Text(action) }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppCacheEntry, max: Long, onClick: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    val fraction by animateFloatAsState(app.cacheBytes.toFloat() / max, tween(800), label = "appBar")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                val bitmap = icon
                if (bitmap != null) {
                    Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                } else {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatBytes(app.cacheBytes, locale), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(4.dp),
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
                Text(
                    text = stringResource(
                        R.string.app_caches_row_detail,
                        formatBytes(app.appBytes, locale),
                        formatBytes(app.dataBytes, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
