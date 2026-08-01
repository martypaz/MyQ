package com.martypaz.myq.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/**
 * A parallax starfield: three layers of stars drifting leftwards at different
 * speeds, so the near layer visibly outruns the far one. Positions are seeded
 * once and remembered, so the field stays stable across recompositions.
 */
@Composable
fun StarField(modifier: Modifier = Modifier) {
    val layers = remember {
        val random = Random(seed = 42) // fixed seed: the same sky every launch
        LAYER_SPECS.map { spec ->
            List(spec.count) {
                Star(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    radius = spec.minRadius + random.nextFloat() * (spec.maxRadius - spec.minRadius),
                    alpha = spec.minAlpha + random.nextFloat() * (1f - spec.minAlpha),
                )
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "starfield")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    // A slow global twinkle keeps the field alive without drawing attention.
    val twinkle by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "twinkle",
    )

    Canvas(modifier = modifier) {
        layers.forEachIndexed { index, stars ->
            val speed = LAYER_SPECS[index].speed
            stars.forEach { star ->
                // Wrap into 0..1 so stars re-enter from the right edge.
                var x = (star.x - drift * speed) % 1f
                if (x < 0f) x += 1f
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha * twinkle),
                    radius = star.radius,
                    center = Offset(x * size.width, star.y * size.height),
                )
            }
        }
    }
}

private data class LayerSpec(
    val count: Int,
    val speed: Float,
    val minRadius: Float,
    val maxRadius: Float,
    val minAlpha: Float,
)

/** Far, mid, near — increasing speed and size is what sells the parallax. */
private val LAYER_SPECS = listOf(
    LayerSpec(count = 110, speed = 0.35f, minRadius = 0.7f, maxRadius = 1.4f, minAlpha = 0.20f),
    LayerSpec(count = 60, speed = 0.85f, minRadius = 1.3f, maxRadius = 2.2f, minAlpha = 0.35f),
    LayerSpec(count = 26, speed = 1.8f, minRadius = 2.0f, maxRadius = 3.2f, minAlpha = 0.55f),
)
