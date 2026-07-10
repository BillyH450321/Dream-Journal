package com.example.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.center
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Liquid glass (glassmorphism) design primitives.
 *
 * The look is achieved with three layers:
 *  1. [LiquidGlassBackground] — a slowly drifting aurora of colored light blobs.
 *  2. Translucent panel fills ([GlassFill]) that let the aurora glow through.
 *  3. Luminous gradient borders + top highlight ([Modifier.liquidGlass]).
 */

// Translucent glass fills
val GlassFill = Color(0x14FFFFFF)          // ~8% white frosted fill
val GlassFillStrong = Color(0x1FFFFFFF)    // ~12% white for emphasized panels
val GlassBorder = Color(0x33FFFFFF)        // ~20% white luminous edge
val GlassHighlight = Color(0x4DFFFFFF)     // ~30% white top-edge shine

/** Gradient brush used as a border to give panels a light-caught glass edge. */
fun glassBorderBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(
            GlassHighlight,
            GlassBorder.copy(alpha = 0.08f),
            DreamPurple.copy(alpha = 0.35f)
        )
    )

/** Subtle vertical sheen laid over glass panels. */
fun glassSheenBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    )

/**
 * Turns any composable into a frosted liquid-glass panel:
 * translucent fill, vertical sheen and a luminous gradient border.
 */
fun Modifier.liquidGlass(shape: Shape, strong: Boolean = false): Modifier = composed {
    this
        .clip(shape)
        .background(if (strong) GlassFillStrong else GlassFill, shape)
        .background(glassSheenBrush(), shape)
        .border(1.dp, glassBorderBrush(), shape)
}

/**
 * Animated aurora backdrop: drifting radial light blobs over the cosmic base,
 * giving glass panels something to refract.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .drawBehind {
                val w = size.width
                val h = size.height

                fun blob(color: Color, cx: Float, cy: Float, radius: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }

                blob(
                    DreamPurple.copy(alpha = 0.28f),
                    cx = w * 0.20f + w * 0.10f * cos(phase),
                    cy = h * 0.15f + h * 0.06f * sin(phase),
                    radius = w * 0.75f
                )
                blob(
                    DreamTeal.copy(alpha = 0.20f),
                    cx = w * 0.85f + w * 0.08f * cos(phase + 2.1f),
                    cy = h * 0.45f + h * 0.08f * sin(phase + 2.1f),
                    radius = w * 0.65f
                )
                blob(
                    DreamGold.copy(alpha = 0.10f),
                    cx = w * 0.35f + w * 0.12f * sin(phase + 4.2f),
                    cy = h * 0.85f + h * 0.05f * cos(phase + 4.2f),
                    radius = w * 0.70f
                )
                blob(
                    DeepVioletAccent.copy(alpha = 0.45f),
                    cx = size.center.x,
                    cy = h * 1.05f,
                    radius = w * 0.9f
                )
            }
    ) {
        content()
    }
}
