package com.wwwescape.devicehousekeeping.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The idle Scan button: a gradient orb that "breathes", with ripples radiating behind it. */
@Composable
fun ScanOrb(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 220.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val transition = rememberInfiniteTransition(label = "orb")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "ripple",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val gradientTurn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "gradient",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val base = this.size.minDimension * 0.33f
                val extra = this.size.minDimension * 0.17f
                for (i in 0 until 3) {
                    val t = (ripple + i / 3f) % 1f
                    drawCircle(
                        color = primary.copy(alpha = (1f - t) * 0.35f),
                        radius = base + extra * t,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        val orbSize = size * 0.62f
        Box(
            modifier = Modifier
                .size(orbSize)
                .scale(pressScale * if (enabled) breathe else 1f)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.sweepGradient(listOf(primary, tertiary, primary))
                    } else {
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                    },
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled) {
                // A soft highlight orbiting the orb keeps the gradient alive without hue shifts.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    rotate(gradientTurn) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                                center = Offset(this.size.width * 0.3f, this.size.height * 0.25f),
                                radius = this.size.minDimension * 0.55f,
                            ),
                        )
                    }
                }
            }
            val content = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = content, modifier = Modifier.size(40.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = content,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** The scanning animation: a radar sweep over concentric rings, with a blip lighting up for each
 * kind of junk as it's found. [content] sits in the middle (the live byte counter). */
@Composable
fun ScanRadar(
    foundCategories: Set<JunkCategory>,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val ring = MaterialTheme.colorScheme.outlineVariant
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "sweep",
    )
    val blink by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "blink",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2
            for (i in 1..3) {
                drawCircle(ring.copy(alpha = 0.7f), radius = radius * i / 3f - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
            rotate(angle) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1f to primary.copy(alpha = 0.45f),
                    ),
                    radius = radius,
                )
                drawLine(
                    color = primary,
                    start = center,
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            foundCategories.forEach { category ->
                // Deterministic spot per category, so blips don't jump between frames.
                val random = Random(category.ordinal * 7919 + 17)
                val a = random.nextFloat() * 2 * PI.toFloat()
                val r = radius * (0.45f + random.nextFloat() * 0.45f)
                val p = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
                drawCircle(tertiary.copy(alpha = 0.25f * blink), radius = 12.dp.toPx(), center = p)
                drawCircle(tertiary, radius = 4.dp.toPx(), center = p)
            }
        }
        Box(
            modifier = Modifier
                .size(size * 0.56f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** A thick progress ring with a glowing head, used while cleaning. */
@Composable
fun CleaningRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "cleanProgress")
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val spin by rememberInfiniteTransition(label = "cleanSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "spin",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            // A faint dashed orbit that keeps moving even if a big folder holds progress still.
            rotate(spin) {
                drawArc(
                    primary.copy(alpha = 0.18f), 0f, 40f, false,
                    Offset(stroke * 1.6f, stroke * 1.6f),
                    Size(this.size.width - stroke * 3.2f, this.size.height - stroke * 3.2f),
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            val sweep = 360f * animated
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(tertiary, primary, tertiary)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                val headAngle = Math.toRadians((-90f + sweep).toDouble())
                val r = arcSize.width / 2
                val head = Offset(center.x + (cos(headAngle) * r).toFloat(), center.y + (sin(headAngle) * r).toFloat())
                drawCircle(primary.copy(alpha = 0.25f), radius = stroke, center = head)
            }
        }
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

private data class Particle(val angle: Float, val distance: Float, val radius: Float, val colorIndex: Int, val delay: Float)

/** The "all clean" moment: a disc pops in, a check mark draws itself, and confetti bursts out. */
@Composable
fun DoneBurst(modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val disc = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        disc.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(220)
        check.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(160)
        burst.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
    }
    val particles = remember {
        val random = Random(42)
        List(36) {
            Particle(
                angle = random.nextFloat() * 2 * PI.toFloat(),
                distance = 0.55f + random.nextFloat() * 0.45f,
                radius = 2.5f + random.nextFloat() * 4f,
                colorIndex = random.nextInt(3),
                delay = random.nextFloat() * 0.25f,
            )
        }
    }
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    val discColor = MaterialTheme.colorScheme.primary
    val checkColor = MaterialTheme.colorScheme.onPrimary
    val halo = MaterialTheme.colorScheme.primaryContainer

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2
        val discRadius = radius * 0.52f * disc.value
        drawCircle(halo, radius = discRadius * 1.25f)
        drawCircle(discColor, radius = discRadius)

        particles.forEach { p ->
            val t = ((burst.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) return@forEach
            val d = radius * (0.5f + 0.5f * p.distance * t)
            val position = Offset(center.x + cos(p.angle) * d, center.y + sin(p.angle) * d)
            drawCircle(colors[p.colorIndex].copy(alpha = 1f - t), radius = p.radius.dp.toPx() * (1f - t * 0.5f), center = position)
        }

        if (check.value > 0f) {
            val s = discRadius
            val path = Path().apply {
                moveTo(center.x - s * 0.42f, center.y + s * 0.02f)
                lineTo(center.x - s * 0.12f, center.y + s * 0.32f)
                lineTo(center.x + s * 0.46f, center.y - s * 0.30f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val segment = Path()
            measure.getSegment(0f, measure.length * check.value, segment, true)
            drawPath(segment, checkColor, style = Stroke(width = s * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
