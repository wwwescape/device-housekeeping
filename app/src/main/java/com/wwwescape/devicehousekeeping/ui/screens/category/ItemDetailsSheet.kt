package com.wwwescape.devicehousekeeping.ui.screens.category

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.ui.components.CategoryBadge
import com.wwwescape.devicehousekeeping.ui.components.RowDivider
import com.wwwescape.devicehousekeeping.ui.components.SafetyChip
import com.wwwescape.devicehousekeeping.ui.components.title
import com.wwwescape.devicehousekeeping.ui.theme.PillShape
import com.wwwescape.devicehousekeeping.util.formatBytes
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemDetailsSheet(item: JunkItem, onExclude: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(item.category)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(itemTitle(item), style = MaterialTheme.typography.titleLarge)
                    Text(item.category.title(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SafetyChip(item.category.safety)
            }

            detailLine(item)?.let { (text, warn) ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (warn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                )
            }

            RowDivider()
            Fact(stringResource(R.string.details_size), formatBytes(item.sizeBytes, locale))
            Fact(
                stringResource(R.string.details_type),
                if (item.isDirectory) {
                    stringResource(R.string.details_type_folder, pluralStringResource(R.plurals.files_count, item.fileCount, item.fileCount))
                } else {
                    stringResource(R.string.details_type_file)
                },
            )
            Fact(
                stringResource(R.string.details_modified),
                DateFormat.getMediumDateFormat(context).format(Date(item.lastModified)) + " " +
                    DateFormat.getTimeFormat(context).format(Date(item.lastModified)),
            )
            Column {
                Text(stringResource(R.string.details_path), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer {
                    Text(
                        item.path,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                TextButton(onClick = { copyPath(context, item.path) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                    Text(stringResource(R.string.action_copy_path), modifier = Modifier.padding(start = 8.dp))
                }
            }

            RowDivider()
            Text(
                stringResource(R.string.details_exclude_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onExclude, shape = PillShape, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Block, contentDescription = null)
                Text(stringResource(R.string.action_exclude), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun copyPath(context: Context, path: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("path", path))
    Toast.makeText(context, R.string.toast_path_copied, Toast.LENGTH_SHORT).show()
}
