package com.wwwescape.devicehousekeeping.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wwwescape.devicehousekeeping.R

private val FAQ = listOf(
    R.string.faq_q_ram to R.string.faq_a_ram,
    R.string.faq_q_caches to R.string.faq_a_caches,
    R.string.faq_q_safe to R.string.faq_a_safe,
    R.string.faq_q_review to R.string.faq_a_review,
    R.string.faq_q_measured to R.string.faq_a_measured,
    R.string.faq_q_access to R.string.faq_a_access,
)

/** Straight answers to what users of cleaner apps usually wonder — including why this one
 * doesn't do some of the things others claim to. */
@Composable
fun FaqDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_faq)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FAQ.forEach { (q, a) ->
                    Column {
                        Text(stringResource(q), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(a),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
