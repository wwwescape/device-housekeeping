package com.wwwescape.devicehousekeeping.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** True on Android TV / Google TV, where the UI is driven by a D-pad instead of touch. */
@Composable
fun isTv(): Boolean =
    LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

/**
 * Lets a read-only card or text block take D-pad focus on TV, with a ring when it has it.
 *
 * A remote can only move between focusable elements, and a scrolling screen only scrolls far
 * enough to show the focused one — so without this, information above the first button (hero
 * cards) or below the last (explanations, history, the FAQ) can never be scrolled into view.
 * Focusing brings the card fully on screen. On touch devices this is a no-op.
 */
@Composable
fun Modifier.tvFocusable(shape: Shape = MaterialTheme.shapes.large): Modifier {
    if (!isTv()) return this
    var focused by remember { mutableStateOf(false) }
    val ring by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "tvFocusRing",
    )
    return this
        .onFocusChanged { focused = it.isFocused }
        .border(3.dp, ring, shape)
        .focusable()
}
