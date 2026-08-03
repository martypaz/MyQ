package com.martypaz.myq.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Glassmorphism tokens. Panels are a translucent white wash that fades down
 * the surface, edged with a light-catching hairline that is brightest at the
 * top-left — the cue that reads as "pane of glass" rather than "grey box".
 */
object Glass {
    val CornerRadius: Dp = 14.dp

    // Fills: a vertical fade sells the thickness of the pane.
    private val RestTop = Color.White.copy(alpha = 0.14f)
    private val RestBottom = Color.White.copy(alpha = 0.05f)
    private val FocusTop = Color.White.copy(alpha = 0.34f)
    private val FocusBottom = Color.White.copy(alpha = 0.18f)

    // Edges: bright where light would land, near-invisible on the far side.
    private val EdgeLit = Color.White.copy(alpha = 0.55f)
    private val EdgeShadow = Color.White.copy(alpha = 0.08f)
    private val EdgeFocused = Color.White.copy(alpha = 0.95f)

    fun fill(focused: Boolean): Brush = Brush.verticalGradient(
        colors = if (focused) listOf(FocusTop, FocusBottom) else listOf(RestTop, RestBottom),
    )

    fun edge(focused: Boolean): Brush = Brush.linearGradient(
        colors = if (focused) {
            listOf(EdgeFocused, Color.White.copy(alpha = 0.45f))
        } else {
            listOf(EdgeLit, EdgeShadow)
        },
    )
}

/**
 * Frosted-pane styling for any surface: translucent gradient fill plus the
 * hairline edge. Focus brightens the pane and thickens its edge, which is how
 * a glass UI signals selection without a heavy solid highlight.
 */
fun Modifier.glass(
    focused: Boolean = false,
    shape: Shape = RoundedCornerShape(Glass.CornerRadius),
): Modifier = this
    .clip(shape)
    .background(brush = Glass.fill(focused), shape = shape)
    .border(width = if (focused) 2.dp else 1.dp, brush = Glass.edge(focused), shape = shape)

/**
 * The colour field the glass sits on. Without something soft and varied
 * behind them, frosted panels have nothing to refract and just look grey.
 *
 * The focused programme's artwork is fetched deliberately tiny and scaled up,
 * which softens it on every Android version; on 12+ a real blur is layered on
 * top, since [Modifier.blur] is a no-op below that.
 */
@Composable
fun GlassBackdrop(imageUrl: String?, modifier: Modifier = Modifier) {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .size(72) // upscaling a thumbnail is our blur on older Android
                .crossfade(700)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (supportsBlur) Modifier.blur(64.dp) else Modifier)
                .alpha(0.38f),
        )

        ColourBlobs(modifier = Modifier.fillMaxSize())

        // Scrim keeps text legible whatever artwork happens to be behind it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xCC0A4D9A),
                        0.55f to Color(0xD905183B),
                        1f to Color(0xF2030A1C),
                    ),
                ),
        )
    }
}

/** Slow, soft pools of brand colour — the light a glass panel refracts. */
@Composable
private fun ColourBlobs(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x662F6BD8), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.16f),
                radius = size.minDimension * 0.75f,
            ),
            radius = size.minDimension * 0.75f,
            center = Offset(size.width * 0.18f, size.height * 0.16f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4D2BB3C4), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.72f),
                radius = size.minDimension * 0.62f,
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.86f, size.height * 0.72f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x332E4FB0), Color.Transparent),
                center = Offset(size.width * 0.52f, size.height * 1.02f),
                radius = size.minDimension * 0.55f,
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.52f, size.height * 1.02f),
        )
    }
}
