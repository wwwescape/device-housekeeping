package com.wwwescape.devicehousekeeping.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.storage.StorageSnapshot
import com.wwwescape.devicehousekeeping.util.formatBytes
import kotlin.math.roundToInt

/**
 * The Home hero: internal storage as a ring that sweeps in on launch, with the reclaimable part of
 * "used" highlighted in the tertiary color (and gently pulsing) once a scan has found junk.
 */
@Composable
fun StorageHero(
    storage: StorageSnapshot,
    reclaimableBytes: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
            ) {
                Text(
                    text = stringResource(R.string.hero_internal_storage).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StorageRing(
                    usedFraction = storage.usedFraction,
                    reclaimableFraction = if (storage.totalBytes > 0) reclaimableBytes.toFloat() / storage.totalBytes else 0f,
                    size = 132.dp,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnimatedBytes(
                        bytes = storage.usedBytes,
                        numberStyle = MaterialTheme.typography.headlineLarge,
                        unitStyle = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        durationMillis = 1200,
                    )
                    Text(
                        text = stringResource(R.string.hero_used_of_total, formatBytes(storage.totalBytes)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 0.dp),
                    )
                    LegendRow(
                        color = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.hero_legend_used),
                        value = formatBytes(storage.usedBytes),
                    )
                    LegendRow(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                        label = stringResource(R.string.hero_legend_free),
                        value = formatBytes(storage.freeBytes),
                    )
                    AnimatedVisibility(
                        visible = reclaimableBytes > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        LegendRow(
                            color = MaterialTheme.colorScheme.tertiary,
                            label = stringResource(R.string.hero_legend_reclaimable),
                            value = formatBytes(reclaimableBytes),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
fun StorageRing(
    usedFraction: Float,
    reclaimableFraction: Float,
    size: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(usedFraction) {
        sweep.animateTo(usedFraction.coerceIn(0f, 1f), tween(1400, easing = FastOutSlowInEasing))
    }
    val junk = remember { Animatable(0f) }
    LaunchedEffect(reclaimableFraction) {
        junk.animateTo(reclaimableFraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    val pulse by rememberInfiniteTransition(label = "junkPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "junkAlpha",
    )
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
    val used = MaterialTheme.colorScheme.primary
    val reclaim = MaterialTheme.colorScheme.tertiary
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val usedSweep = 360f * sweep.value
            if (usedSweep > 0f) {
                drawArc(used, -90f, usedSweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            // The reclaimable slice sits at the end of "used": it's part of it, and is what
            // cleaning will hand back to "free".
            val junkSweep = (360f * junk.value).coerceAtMost(usedSweep)
            if (junkSweep > 0.5f) {
                drawArc(
                    reclaim.copy(alpha = pulse),
                    -90f + usedSweep - junkSweep,
                    junkSweep.coerceAtLeast(2f),
                    false,
                    topLeft,
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(sweep.value * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
            )
            Text(
                text = stringResource(R.string.hero_ring_used),
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.75f),
            )
        }
    }
}
