package com.kasir.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Holds the pieces needed to give a tappable element a "springy" press:
 * scale the element down while pressed so taps feel tactile even before the
 * action fires. Pass [interactionSource] to the Material component and apply
 * [modifier] to it.
 */
class PressScale internal constructor(
    val interactionSource: MutableInteractionSource,
    val modifier: Modifier
)

/**
 * Builds an interaction source + scale-down modifier. Use with `Surface(onClick)`,
 * `Button`, `OutlinedButton`, etc.:
 *
 * ```
 * val press = rememberPressScale()
 * Button(
 *     onClick = { ... },
 *     interactionSource = press.interactionSource,
 *     modifier = press.modifier.fillMaxWidth()
 * ) { ... }
 * ```
 */
@Composable
fun rememberPressScale(scaleTo: Float = 0.94f): PressScale {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        label = "pressScale"
    )
    val modifier = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
    return PressScale(interactionSource, modifier)
}
