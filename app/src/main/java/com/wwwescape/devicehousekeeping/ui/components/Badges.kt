package com.wwwescape.devicehousekeeping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.Safety
import com.wwwescape.devicehousekeeping.ui.theme.PillShape

/** A rounded-square icon tile in the category's own tone. */
@Composable
fun CategoryBadge(category: JunkCategory, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val (container, content) = category.badgeColors()
    Box(
        modifier = modifier
            .size(size)
            .background(container, RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun SafetyChip(safety: Safety, modifier: Modifier = Modifier) {
    val (container, content) = when (safety) {
        Safety.SAFE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        Safety.REVIEW -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f) to MaterialTheme.colorScheme.tertiary
    }
    Surface(shape = PillShape, color = container, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (safety == Safety.SAFE) Icons.Rounded.VerifiedUser else Icons.Rounded.Info,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = safety.label(),
                style = MaterialTheme.typography.labelSmall,
                color = content,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** A small pill holding a fact ("3 days old", "12 files"). */
@Composable
fun MetaPill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    Surface(shape = PillShape, color = color, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A tonal card asking for an access the app needs, with one action. */
@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            FilledTonalButton(
                onClick = onClick,
                shape = PillShape,
                modifier = Modifier.align(Alignment.End),
            ) { Text(action) }
        }
    }
}
