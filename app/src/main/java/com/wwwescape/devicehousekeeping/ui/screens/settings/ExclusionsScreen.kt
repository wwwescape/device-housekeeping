package com.wwwescape.devicehousekeeping.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.ui.components.EmptyStateNotice

@Composable
fun ExclusionsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val paths = settings.cleaning.excludedPaths.sorted()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.exclusions_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (paths.isEmpty()) {
                item(key = "empty") {
                    EmptyStateNotice(
                        title = stringResource(R.string.exclusions_empty_title),
                        body = stringResource(R.string.exclusions_empty_body),
                        icon = Icons.Rounded.Block,
                    )
                }
            }
            items(paths, key = { it }) { path ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(modifier = Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            path,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        )
                        IconButton(onClick = { viewModel.removeExcludedPath(path) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove))
                        }
                    }
                }
            }
        }
    }
}
