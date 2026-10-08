package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurveMath
import com.example.model.CurvePoint
import com.example.model.CurveState
import com.example.ui.theme.NoirAccentMuted
import com.example.ui.theme.NoirAccentSilver
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderLight
import com.example.ui.theme.NoirPitchBlack
import com.example.ui.theme.NoirSurface
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirSurfaceHighlight
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirTextTertiary
import kotlin.math.roundToInt

@Composable
fun CurvesEditorView(
    curveState: CurveState,
    onCurveChange: (CurveState, Boolean) -> Unit, // Boolean: isFinished (true when drag ends)
    onResetCurve: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activePointIndex by remember { mutableIntStateOf(-1) }
    val currentPoints = curveState.points
    val updatedPointsState by rememberUpdatedState(currentPoints)

    val pointLabels = listOf("Blacks", "Shadows", "Midtones", "Highlights", "Whites")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NoirSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("curves_editor_panel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar: < Back / Cancel, CURVES, RESET, DONE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDone,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_curves_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Curves",
                        tint = NoirAccentWhite
                    )
                }

                Text(
                    text = "TONAL CURVES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoirAccentWhite,
                        fontSize = 13.sp
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetCurve,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NoirTextSecondary
                    ),
                    border = BorderStroke(1.dp, NoirBorder),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("reset_curve_button")
                ) {
                    Text(
                        text = "RESET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    )
                }

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoirAccentWhite,
                        contentColor = NoirPitchBlack
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("done_curves_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NoirPitchBlack,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "DONE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Node Coordinate Readout Banner
        val activeLabel = if (activePointIndex in currentPoints.indices) {
            val pt = currentPoints[activePointIndex]
            val inVal = (pt.x * 255f).roundToInt()
            val outVal = (pt.y * 255f).roundToInt()
            val name = pointLabels.getOrElse(activePointIndex) { "Node" }
            "$name • Input: $inVal (${(pt.x * 100).toInt()}%) → Output: $outVal (${(pt.y * 100).toInt()}%)"
        } else {
            "Touch and drag any node to sculpt tonal graduation"
        }

        Text(
            text = activeLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (activePointIndex >= 0) NoirAccentWhite else NoirTextTertiary
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Interactive 256x256 Square Chart with Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Vertical Axis Label (Output 1.0 down to 0.0)
            Column(
                modifier = Modifier
                    .height(200.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "1.0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
                Text(text = "0.75", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
                Text(text = "0.50", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
                Text(text = "0.25", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
                Text(text = "0.0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
            }

            // Chart Canvas Box
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(NoirPitchBlack, RoundedCornerShape(2.dp))
                    .border(1.dp, NoirBorderLight, RoundedCornerShape(2.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("curve_canvas")
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width
                                    val h = size.height
                                    val pts = updatedPointsState
                                    var closestDist = Float.MAX_VALUE
                                    var closestIdx = -1

                                    // Touch target: generous 48dp radius
                                    val hitRadiusPx = 48.dp.toPx()
                                    val maxHitDistSq = hitRadiusPx * hitRadiusPx

                                    pts.forEachIndexed { index, pt ->
                                        val px = pt.x * w
                                        val py = (1f - pt.y) * h
                                        val dist = (offset.x - px) * (offset.x - px) + (offset.y - py) * (offset.y - py)
                                        if (dist <= maxHitDistSq && dist < closestDist) {
                                            closestDist = dist
                                            closestIdx = index
                                        }
                                    }
                                    activePointIndex = closestIdx
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (activePointIndex in updatedPointsState.indices) {
                                        val h = size.height
                                        val newY = (1f - (change.position.y / h)).coerceIn(0.0f, 1.0f)
                                        val pts = updatedPointsState.toMutableList()
                                        val cur = pts[activePointIndex]

                                        // Update Y while keeping X strictly anchored or clamped
                                        pts[activePointIndex] = cur.copy(y = newY)
                                        onCurveChange(CurveState(pts), false)
                                    }
                                },
                                onDragEnd = {
                                    activePointIndex = -1
                                    onCurveChange(CurveState(updatedPointsState), true)
                                },
                                onDragCancel = {
                                    activePointIndex = -1
                                    onCurveChange(CurveState(updatedPointsState), true)
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Grid lines (25%, 50%, 75%)
                    val gridColor = Color(0xFF222224)
                    for (i in 1..3) {
                        val frac = i * 0.25f
                        // Vertical grid line (Input divisions)
                        drawLine(
                            color = gridColor,
                            start = Offset(frac * w, 0f),
                            end = Offset(frac * w, h),
                            strokeWidth = 1f
                        )
                        // Horizontal grid line (Output divisions)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, frac * h),
                            end = Offset(w, frac * h),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Identity Diagonal Line (0,0) to (1,1) (dashed subtle guide)
                    drawLine(
                        color = Color(0xFF38383C),
                        start = Offset(0f, h),
                        end = Offset(w, 0f),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // 3. Render Smooth Fritsch-Carlson Monotone Spline from LUT
                    val lut = CurveMath.computeLut(currentPoints)
                    val path = Path()
                    path.moveTo(0f, h - (lut[0] / 255f) * h)

                    for (i in 1..255) {
                        val px = (i / 255f) * w
                        val py = h - (lut[i] / 255f) * h
                        path.lineTo(px, py)
                    }

                    drawPath(
                        path = path,
                        color = NoirAccentWhite,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 4. Render Control Point Nodes
                    currentPoints.forEachIndexed { index, pt ->
                        val cx = pt.x * w
                        val cy = (1f - pt.y) * h
                        val isSelected = index == activePointIndex

                        // Outer generous glow/halo for easy touch tracking
                        drawCircle(
                            color = if (isSelected) Color(0x88FFFFFF) else Color(0x33FFFFFF),
                            radius = if (isSelected) 12.dp.toPx() else 8.dp.toPx(),
                            center = Offset(cx, cy)
                        )

                        // Middle white circle
                        drawCircle(
                            color = NoirAccentWhite,
                            radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )

                        // Center dark core
                        drawCircle(
                            color = NoirPitchBlack,
                            radius = 2.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }

        // Horizontal Axis Label (Input 0.0 to 1.0)
        Row(
            modifier = Modifier
                .width(200.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0.0 (Input)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
            Text(text = "0.5", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
            Text(text = "1.0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = NoirTextTertiary))
        }
    }
}
