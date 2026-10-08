package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirDark
import com.example.ui.theme.NoirPitchBlack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ZoomableImageView(
    originalBitmap: Bitmap?,
    editedBitmap: Bitmap?,
    scale: Float,
    offset: Offset,
    onTransformChange: (Float, Offset) -> Unit,
    splitPosition: Float?, // null = normal single image, 0.0f .. 1.0f = before/after split
    onSplitChange: (Float) -> Unit,
    isHoldComparing: Boolean, // true = force show original
    modifier: Modifier = Modifier
) {
    val currentSplitPositionState by rememberUpdatedState(splitPosition)
    val currentScaleState by rememberUpdatedState(scale)
    val currentOffsetState by rememberUpdatedState(offset)

    val activeDisplayBitmap = if (isHoldComparing) originalBitmap else editedBitmap

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NoirPitchBlack)
            .clipToBounds()
            .testTag("preview_viewport")
            // Double-tap gesture: toggles between 1.0x (Fit to Screen) and 2.5x (100% inspection)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val s = currentScaleState
                        if (s > 1.2f) {
                            onTransformChange(1.0f, Offset.Zero)
                        } else {
                            onTransformChange(2.5f, Offset.Zero)
                        }
                    }
                )
            }
            // Continuous Touch Gesture Pipeline
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val split = currentSplitPositionState

                        if (split != null) {
                            // SPLIT MODE ACTIVE
                            if (event.changes.size == 1) {
                                // Single-finger drag: dedicated divider movement
                                val change = event.changes.first()
                                if (change.pressed) {
                                    val newPos = (change.position.x / size.width.toFloat()).coerceIn(0.0f, 1.0f)
                                    onSplitChange(newPos)
                                    change.consume()
                                }
                            } else if (event.changes.size >= 2) {
                                // Two-finger pinch: zoom & pan in split mode
                                var prevPointers = event.changes
                                while (event.changes.any { it.pressed }) {
                                    val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                                    if (nextEvent.changes.size >= 2) {
                                        val p0 = nextEvent.changes[0].position
                                        val p1 = nextEvent.changes[1].position
                                        val oldP0 = nextEvent.changes[0].previousPosition
                                        val oldP1 = nextEvent.changes[1].previousPosition

                                        val currentDistance = (p0 - p1).getDistance()
                                        val previousDistance = (oldP0 - oldP1).getDistance()

                                        if (previousDistance > 0f) {
                                            val zoomDelta = currentDistance / previousDistance
                                            val panDelta = ((p0 + p1) / 2f) - ((oldP0 + oldP1) / 2f)

                                            val curScale = currentScaleState
                                            val curOffset = currentOffsetState

                                            val newScale = (curScale * zoomDelta).coerceIn(1.0f, 6.0f)
                                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                                            val maxPanY = (size.height * (newScale - 1f)) / 2f

                                            val newOffsetX = if (newScale > 1f) (curOffset.x + panDelta.x).coerceIn(-maxPanX, maxPanX) else 0f
                                            val newOffsetY = if (newScale > 1f) (curOffset.y + panDelta.y).coerceIn(-maxPanY, maxPanY) else 0f

                                            onTransformChange(newScale, Offset(newOffsetX, newOffsetY))
                                            nextEvent.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                        } else {
                            // NORMAL SINGLE-VIEW MODE (Zoom & Pan)
                            if (event.changes.size >= 2) {
                                // Two-finger pinch zoom
                                while (event.changes.any { it.pressed }) {
                                    val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                                    if (nextEvent.changes.size >= 2) {
                                        val p0 = nextEvent.changes[0].position
                                        val p1 = nextEvent.changes[1].position
                                        val oldP0 = nextEvent.changes[0].previousPosition
                                        val oldP1 = nextEvent.changes[1].previousPosition

                                        val currentDist = (p0 - p1).getDistance()
                                        val prevDist = (oldP0 - oldP1).getDistance()

                                        if (prevDist > 0f) {
                                            val zoom = currentDist / prevDist
                                            val pan = ((p0 + p1) / 2f) - ((oldP0 + oldP1) / 2f)

                                            val curScale = currentScaleState
                                            val curOffset = currentOffsetState

                                            val newScale = (curScale * zoom).coerceIn(1.0f, 6.0f)
                                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                                            val maxPanY = (size.height * (newScale - 1f)) / 2f

                                            val newOffsetX = if (newScale > 1f) (curOffset.x + pan.x).coerceIn(-maxPanX, maxPanX) else 0f
                                            val newOffsetY = if (newScale > 1f) (curOffset.y + pan.y).coerceIn(-maxPanY, maxPanY) else 0f

                                            onTransformChange(newScale, Offset(newOffsetX, newOffsetY))
                                            nextEvent.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            } else if (event.changes.size == 1 && currentScaleState > 1.0f) {
                                // Single-finger pan when zoomed in
                                val change = event.changes.first()
                                val panDelta = change.position - change.previousPosition

                                val curScale = currentScaleState
                                val curOffset = currentOffsetState

                                val maxPanX = (size.width * (curScale - 1f)) / 2f
                                val maxPanY = (size.height * (curScale - 1f)) / 2f

                                val newOffsetX = (curOffset.x + panDelta.x).coerceIn(-maxPanX, maxPanX)
                                val newOffsetY = (curOffset.y + panDelta.y).coerceIn(-maxPanY, maxPanY)

                                onTransformChange(curScale, Offset(newOffsetX, newOffsetY))
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        val origImageBitmap = remember(originalBitmap) { originalBitmap?.asImageBitmap() }
        val editImageBitmap = remember(editedBitmap) { editedBitmap?.asImageBitmap() }
        val displayImageBitmap = remember(activeDisplayBitmap) { activeDisplayBitmap?.asImageBitmap() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Calculate exact fitted aspect-ratio rectangle preserving 100% of image proportions
            val refBitmap = editedBitmap ?: originalBitmap
            if (refBitmap == null) return@Canvas

            val imgW = refBitmap.width.toFloat()
            val imgH = refBitmap.height.toFloat()
            val imgAspect = imgW / imgH
            val containerAspect = canvasW / canvasH

            val (fitW, fitH) = if (containerAspect > imgAspect) {
                // Container is wider than image: fit to height
                val h = canvasH
                val w = h * imgAspect
                Pair(w, h)
            } else {
                // Container is taller than image: fit to width
                val w = canvasW
                val h = w / imgAspect
                Pair(w, h)
            }

            val fitLeft = (canvasW - fitW) / 2f
            val fitTop = (canvasH - fitH) / 2f

            val dstOffset = IntOffset(fitLeft.roundToInt(), fitTop.roundToInt())
            val dstSize = IntSize(fitW.roundToInt(), fitH.roundToInt())

            // Render with zoom scale & translation applied uniformly around center
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset(canvasW / 2f, canvasH / 2f))
            }) {
                if (splitPosition == null) {
                    // 1. Single View (Edited or Hold-Original)
                    displayImageBitmap?.let { img ->
                        drawImage(
                            image = img,
                            dstOffset = dstOffset,
                            dstSize = dstSize
                        )
                    }
                } else {
                    // 2. Draggable Split Before/After View
                    // Both images drawn at identical coordinates (dstOffset, dstSize) - zero stretching!
                    val splitX = canvasW * splitPosition

                    // Left Side: ORIGINAL PHOTOGRAPH
                    origImageBitmap?.let { orig ->
                        clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasH) {
                            drawImage(
                                image = orig,
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )
                        }
                    }

                    // Right Side: EDITED NOIR PHOTOGRAPH
                    editImageBitmap?.let { edit ->
                        clipRect(left = splitX, top = 0f, right = canvasW, bottom = canvasH) {
                            drawImage(
                                image = edit,
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )
                        }
                    }
                }
            }

            // Divider Line and Draggable Handle (drawn on top of the viewport in screen coordinates)
            if (splitPosition != null) {
                val splitX = canvasW * splitPosition

                // Clean vertical divider line
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, 0f),
                    end = Offset(splitX, canvasH),
                    strokeWidth = 2.dp.toPx()
                )

                // Draggable knob indicator at vertical center
                val knobCenter = Offset(splitX, canvasH / 2f)
                drawCircle(
                    color = Color.White,
                    radius = 18.dp.toPx(),
                    center = knobCenter
                )
                drawCircle(
                    color = NoirDark,
                    radius = 14.dp.toPx(),
                    center = knobCenter
                )

                // Inner arrows indicator lines: ◄ ►
                drawLine(
                    color = Color.White,
                    start = Offset(splitX - 6.dp.toPx(), canvasH / 2f),
                    end = Offset(splitX - 2.dp.toPx(), canvasH / 2f - 4.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(splitX - 6.dp.toPx(), canvasH / 2f),
                    end = Offset(splitX - 2.dp.toPx(), canvasH / 2f + 4.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(splitX + 6.dp.toPx(), canvasH / 2f),
                    end = Offset(splitX + 2.dp.toPx(), canvasH / 2f - 4.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawLine(
                    color = Color.White,
                    start = Offset(splitX + 6.dp.toPx(), canvasH / 2f),
                    end = Offset(splitX + 2.dp.toPx(), canvasH / 2f + 4.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx()
                )
            }
        }

        // Split Mode Badges: ORIGINAL | EDITED
        if (splitPosition != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ORIGINAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = if (splitPosition > 0.5f) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        color = if (splitPosition > 0.5f) NoirAccentWhite else Color(0x99FFFFFF)
                    )
                )
                Text(
                    text = "  |  ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0x66FFFFFF)
                    )
                )
                Text(
                    text = "EDITED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = if (splitPosition <= 0.5f) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        color = if (splitPosition <= 0.5f) NoirAccentWhite else Color(0x99FFFFFF)
                    )
                )
            }
        }

        // Hold-to-Compare Banner
        if (isHoldComparing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(2.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SHOWING ORIGINAL (UNEDITED)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        color = NoirAccentWhite
                    )
                )
            }
        }
    }
}
