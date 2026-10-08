package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NoirPitchBlack

/**
 * "The Eclipse Eye" - Minimalist geometric emblem for UMBRA.
 * Symmetrical union of an eclipse, human eye, shadow, and negative space.
 */
@Composable
fun EclipseEyeLogo(
    size: Dp = 64.dp,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Outer Eye Contour (Almond curvature)
        val eyePath = Path().apply {
            moveTo(w * 0.12f, cy)
            cubicTo(
                w * 0.26f, cy - h * 0.35f,
                w * 0.40f, cy - h * 0.44f,
                cx, cy - h * 0.44f
            )
            cubicTo(
                w * 0.60f, cy - h * 0.44f,
                w * 0.74f, cy - h * 0.35f,
                w * 0.88f, cy
            )
            cubicTo(
                w * 0.74f, cy + h * 0.35f,
                w * 0.60f, cy + h * 0.44f,
                cx, cy + h * 0.44f
            )
            cubicTo(
                w * 0.40f, cy + h * 0.44f,
                w * 0.26f, cy + h * 0.35f,
                w * 0.12f, cy
            )
            close()
        }

        drawPath(
            path = eyePath,
            color = tint,
            style = Stroke(
                width = w * 0.055f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // The Eclipse Corona / Outer Iris (Solid White Circle)
        val eclipseRadius = w * 0.24f
        drawCircle(
            color = tint,
            radius = eclipseRadius,
            center = Offset(cx, cy)
        )

        // Total Eclipse Shadow / Negative Space Pupil
        val pupilRadius = w * 0.12f
        drawCircle(
            color = NoirPitchBlack,
            radius = pupilRadius,
            center = Offset(cx, cy)
        )

        // Singular pinpoint focal light (Catchlight)
        val catchlightRadius = w * 0.025f
        drawCircle(
            color = tint,
            radius = catchlightRadius,
            center = Offset(cx + w * 0.02f, cy - h * 0.04f)
        )
    }
}
